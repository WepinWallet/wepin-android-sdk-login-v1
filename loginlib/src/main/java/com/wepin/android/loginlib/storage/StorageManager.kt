package com.wepin.android.loginlib.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.Providers
import com.wepin.android.loginlib.types.StorageDataType
import com.wepin.android.loginlib.types.Token
import com.wepin.android.loginlib.types.UserInfo
import com.wepin.android.loginlib.types.UserInfoDetails
import com.wepin.android.loginlib.types.UserStatus
import com.wepin.android.loginlib.types.WepinLoginStatus
import com.wepin.android.loginlib.types.WepinUser
import com.wepin.android.loginlib.types.network.LoginResponse
import com.wepin.android.loginlib.utils.convertJsonToLocalStorageData
import com.wepin.android.loginlib.utils.convertLocalStorageDataToJson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.text.Charsets.UTF_8

internal object StorageManager {
    private var _appId: String = ""
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    private const val PREFERENCE_NAME = "wepin_encrypted_preferences"
    private lateinit var encDataPair: Pair<ByteArray, ByteArray>
    private lateinit var sharedPreferences: EncryptedSharedPreferences

    fun init(context: Context, appId: String) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            this._appId = appId
            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFERENCE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences

            val key = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keySpec = KeyGenParameterSpec.Builder(
                PREFERENCE_NAME + appId,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(false)
                .build()
            key.init(keySpec)
            key.generateKey()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Set EncryptedSharedPreferences
    fun setStorage(key: String, data: Any) {
        if (this::sharedPreferences.isInitialized) {
            val appSpecificKey = "${_appId}_$key"
            when (data) {
                is StorageDataType -> {
                    val stringData = convertLocalStorageDataToJson(data)
                    sharedPreferences.edit().putString(appSpecificKey, stringData)?.apply()
                    encDataPair = getEncryptedDataPair(stringData)
                    sharedPreferences.edit().putString(appSpecificKey, stringData)?.apply()
                    encDataPair = getEncryptedDataPair(stringData)
                    encDataPair.second.toString(UTF_8)
                }
                is String -> {
                    sharedPreferences.edit().putString(appSpecificKey, data)?.apply()
                    encDataPair = getEncryptedDataPair(data)
                    sharedPreferences.edit().putString(appSpecificKey, data)?.apply()
                    encDataPair = getEncryptedDataPair(data)
                    encDataPair.second.toString(UTF_8)
                }
                else -> {
                    throw IllegalArgumentException("Unsupported data type")
                }
            }
        }
    }

    // Get EncryptedSharedPreferences
    fun getStorage(key: String): Any? {
        var stringData: String? = null
        try {
            if (this::sharedPreferences.isInitialized) {
                val appSpecificKey = "${_appId}_$key"
                val stringRes = sharedPreferences.getString(appSpecificKey, null) ?: return null
                val encryptedPairData = getEncryptedDataPair(stringRes)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val keySpec = IvParameterSpec(encryptedPairData.first)
                cipher.init(Cipher.DECRYPT_MODE, getKey(), keySpec)
                stringData = cipher.doFinal(encryptedPairData.second).toString(UTF_8)
                return convertJsonToLocalStorageData(stringData)
            }
        } catch (e: Exception) {
//            e.printStackTrace()
            return stringData
        }
        return null
    }

    // Delete EncryptedSharedPreferences
    fun deleteStorage(key: String) {
        val appSpecificKey = "${_appId}_$key"
        sharedPreferences.edit().remove(appSpecificKey).apply()
    }

    // Delete All EncryptedSharedPreferences for a specific appId
    fun deleteAllStorageWithAppId() {
        val sharedPreferenceIds = sharedPreferences.all
        sharedPreferenceIds.forEach {
            if (it.key.startsWith(_appId)) {
                sharedPreferences.edit().remove(it.key).apply()
            }
        }
    }

    // Delete All EncryptedSharedPreferences regardless of appId
    fun deleteAllStorage() {
        val sharedPreferenceIds = sharedPreferences.all
        sharedPreferenceIds.forEach {
            sharedPreferences.edit().remove(it.key).apply()
        }
    }

    // Check if appId related data exists
    private fun isAppIdDataExists(): Boolean {
        val sharedPreferenceIds = sharedPreferences.all
        sharedPreferenceIds.forEach {
            if (it.key.startsWith(_appId)) {
                return true
            }
        }
        return false
    }

    // Delete all data if appId data does not exist
    fun deleteAllIfAppIdDataNotExists() {
        if (!isAppIdDataExists()) {
            deleteAllStorage()
        }
    }

    // Get all EncryptedSharedPreferences
    fun getAllStorage(): Map<String, Any?> {
        val allData = mutableMapOf<String, Any?>()
        val sharedPreferenceIds = sharedPreferences.all
        sharedPreferenceIds.forEach {
            if (it.key.startsWith(_appId)) {
                val key = it.key.removePrefix("${_appId}_")
                allData[key] = getStorage(key)
            }
        }
        return allData
    }

    // Set all EncryptedSharedPreferences
    fun setAllStorage(data: Map<String, Any>) {
        data.forEach { (key, value) ->
            setStorage(key, value)
        }
    }

    private fun getEncryptedDataPair(data: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())

        val iv: ByteArray = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Pair(iv, encryptedData)
    }

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secreteKeyEntry: KeyStore.SecretKeyEntry =
            keyStore.getEntry(PREFERENCE_NAME + this._appId, null) as KeyStore.SecretKeyEntry
        return secreteKeyEntry.secretKey
    }
    fun setFirebaseUser(lgoinResult: LoginResult) {
        deleteAllStorage()
        setStorage("firebase:wepin",
            StorageDataType.FirebaseWepin(
                idToken = lgoinResult.token.idToken,
                refreshToken = lgoinResult.token.refreshToken,
                provider = lgoinResult.provider.value
            )
        )
    }

    fun getWepinUser(): WepinUser? {
        val walletId = getStorage("wallet_id")
        val userInfo = getStorage("user_info")
        val wepinToken = getStorage("wepin:connectUser")
        val userStatus = getStorage("user_status")

        if (userInfo == null || wepinToken == null || userStatus == null) {
            return null
        }
        var wepinWallet: String? = null
        if(walletId != null){
            wepinWallet = walletId as String
        }
        val wepinUser = WepinUser(
            status = "success",
            userInfo = UserInfo(
                userId = (userInfo as StorageDataType.UserInfo).userInfo!!.userId,
                email = (userInfo as StorageDataType.UserInfo).userInfo!!.email,
                provider = Providers.fromValue((userInfo as StorageDataType.UserInfo).userInfo!!.provider)!!,//Providers.fromValue(params.provider)!!,
                use2FA = (userInfo as StorageDataType.UserInfo).userInfo!!.use2FA
            ),
            userStatus = UserStatus(
                loginStatus= WepinLoginStatus.fromValue((userStatus as StorageDataType.UserStatus).loginStatus)!!,
                pinRequired = (userStatus as StorageDataType.UserStatus).pinRequired
            ),
            walletId = wepinWallet,
            token = Token(
                accessToken = (wepinToken as StorageDataType.WepinToken).accessToken,
                refreshToken = (wepinToken as StorageDataType.WepinToken).refreshToken
            )
        )
        return wepinUser
    }
    fun setWepinUser(request: LoginResult,
                     response: LoginResponse) {
        deleteAllStorage()
        setStorage("firebase:wepin",
            StorageDataType.FirebaseWepin(
                idToken = request.token.idToken,
                refreshToken = request.token.refreshToken,
                provider = request.provider.value
            )
        )
        setStorage("wepin:connectUser",
            StorageDataType.WepinToken(
                accessToken = response.token.access,
                refreshToken = response.token.refresh,
            )
        )

        setStorage("user_id", response.userInfo.userId)

        setStorage("user_status",
            StorageDataType.UserStatus(
                loginStatus = response.loginStatus,
                pinRequired = (if (response.loginStatus == "registerRequired") response.pinRequired else false),
            )
        )

        if (response.loginStatus != "pinRequired" && response.walletId != null) {
            setStorage("wallet_id", response.walletId)
            setStorage("user_info",
                StorageDataType.UserInfo(
                    status = "success",
                    userInfo = UserInfoDetails(
                        userId = response.userInfo.userId,
                        email = response.userInfo.email,
                        provider = request.provider.value,
                        use2FA = (response.userInfo.use2FA >= 2),
                    ),
                    walletId = response.walletId
                )
            )
        }else {
            val userInfo = StorageDataType.UserInfo(
                status = "success",
                userInfo = UserInfoDetails(
                    userId = response.userInfo.userId,
                    email = response.userInfo.email,
                    provider = request.provider.value,
                    use2FA = (response.userInfo.use2FA >= 2),
                ),
            )
            setStorage("user_info", userInfo)
        }
        setStorage("oauth_provider_pending", request.provider.value)
    }
}
