package com.wepin.android.loginlib.storage

import android.content.Context
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

internal object WepinStorageManager {
    private const val PREV_PREFERENCE_NAME = "wepin_encrypted_preferences"
    private lateinit var _prevStorage: EncryptedSharedPreferences
    private lateinit var _storage: StorageManager
    private var _appId: String = ""

    fun init(context: Context, appId: String) {
        this._appId = appId

        try {
            initializePrevStorage(context)
        } catch(error: Exception) {
            context.getSharedPreferences(PREV_PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            initializePrevStorage(context)
        }
        _storage = StorageManager(context)
        migrationOldStorage()
    }

    private fun initializePrevStorage(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        _prevStorage = EncryptedSharedPreferences.create(
            context,
            PREV_PREFERENCE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    private fun migrationOldStorage() {
        try {
            val migrationState = getStorage<Boolean>("migration")
            if (migrationState == true) return

            val oldStorage = _prevStorageReadAll()
            oldStorage?.forEach { (key, value) ->
                setStorage(key, value)
            }
        } catch(e: Exception) {
//            println("Migration failed with an unexpected error - $e")
        } finally {
            setStorage("migration", true)
            _prevDeleteAll()
        }
    }

    fun <T> _encodeValue(value: T): String {
        return when (value) {
            is String,
            is Int,
            is Double,
            is Boolean -> {
                value.toString()
            }

            is StorageDataType -> {
                convertLocalStorageDataToJson(value)
            }

            else -> throw IllegalArgumentException("Unsupported data type")
        }
    }

    fun <T> _parseValue(value: String): T? {
        val primitiveValue: Any? = when {
            value.equals("true", ignoreCase = true) -> true
            value.equals("false", ignoreCase = true) -> false
            value.toIntOrNull() != null -> value.toInt()
            value.toDoubleOrNull() != null -> value.toDouble()
            else -> null
        }

        @Suppress("UNCHECKED_CAST")
        return try {
            if (primitiveValue != null) {
                primitiveValue as? T
            } else {
                convertJsonToLocalStorageData(value) as? T ?: value as? T
            }
        } catch (e: Exception) {
            //String
            value as? T
        }
    }


    // Set EncryptedSharedPreferences
    fun <T> setStorage(key: String, data: T) {
            try {
                val stringValue = _encodeValue(data)
                _storage.write(_appId, key, stringValue)
            } catch(e: Exception) {
                if (!e.message.toString().contains("already exists")){
                    throw e
                }
            }
    }

    // Get EncryptedSharedPreferences
    fun <T> getStorage(key: String): T? {
        return try {
            _storage.read(_appId, key)?.let { _parseValue(it) }
        } catch (e: Exception) {
            null
        }
    }
//
//    // Delete EncryptedSharedPreferences
//    fun deleteStorage(key: String) {
//        try {
//            _storage.delete(_appId, key)
//        } catch (e: Exception) {
//            println("")
//        }
//    }

    // Delete All EncryptedSharedPreferences for a specific appId
//    fun deleteAllStorageWithAppId() {
//        val sharedPreferenceIds = sharedPreferences.all
//        sharedPreferenceIds.forEach {
//            if (it.key.startsWith(_appId)) {
//                sharedPreferences.edit().remove(it.key).apply()
//            }
//          }
//    }

    // Delete All EncryptedSharedPreferences regardless of appId
    fun deleteAllStorage() {
        _storage.deleteAll()

        setStorage("migration", true);
    }

    // Check if appId related data exists
//    private fun isAppIdDataExists(): Boolean {
//        val sharedPreferenceIds = sharedPreferences.all
//        sharedPreferenceIds.forEach {
//            if (it.key.startsWith(_appId)) {
//                return true
//            }
//        }
//        return false
//    }

    // Delete all data if appId data does not exist
//    fun deleteAllIfAppIdDataNotExists() {
//        if (!isAppIdDataExists()) {
//            deleteAllStorage()
//        }
//    }

    // Get all EncryptedSharedPreferences
    fun getAllStorage(): Map<String, Any?>? {
        try {
            val allData = _storage.readAll(_appId)

            val filteredData = mutableMapOf<String, Any?>()
            for (key in allData.keys) {
                val storageKey = key.replaceFirst("${_appId}_", "")
                try {
                    val jsonValue = _parseValue<StorageDataType>(allData[key] ?: "")
                    filteredData[storageKey] = jsonValue
                } catch (e: Exception) {
                    filteredData[storageKey] = allData[key]
                }
            }
            return if (filteredData.isEmpty()) null else filteredData

        } catch (e: Exception) {
            return null
        }
    }

    // Set all EncryptedSharedPreferences
    fun setAllStorage(data: Map<String, Any>) {
        data.forEach { (key, value) ->
            setStorage(key, value)
        }
    }

    fun setFirebaseUser(loginResult: LoginResult) {
        deleteAllStorage()
        setStorage<StorageDataType>("firebase:wepin",
            StorageDataType.FirebaseWepin(
                idToken = loginResult.token.idToken,
                refreshToken = loginResult.token.refreshToken,
                provider = loginResult.provider.value
            )
        )
    }

    fun getWepinUser(): WepinUser? {
        try {
            val walletId = getStorage<String>("wallet_id")
            val userInfo = getStorage<StorageDataType>("user_info")
            val wepinToken = getStorage<StorageDataType>("wepin:connectUser")
            val userStatus = getStorage<StorageDataType>("user_status")

            if (userInfo == null || wepinToken == null || userStatus == null) {
                return null
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
                walletId = walletId,
                token = Token(
                    accessToken = (wepinToken as StorageDataType.WepinToken).accessToken,
                    refreshToken = (wepinToken as StorageDataType.WepinToken).refreshToken
                )
            )
            return wepinUser
        }catch (e: Exception){
            e.printStackTrace()
        }
        return null
    }
    fun setWepinUser(request: LoginResult,
                     response: LoginResponse) {
        deleteAllStorage()
        setStorage<StorageDataType>("firebase:wepin",
            StorageDataType.FirebaseWepin(
                idToken = request.token.idToken,
                refreshToken = request.token.refreshToken,
                provider = request.provider.value
            )
        )
        setStorage<StorageDataType>("wepin:connectUser",
            StorageDataType.WepinToken(
                accessToken = response.token.access,
                refreshToken = response.token.refresh,
            )
        )

        setStorage("user_id", response.userInfo.userId)

        setStorage<StorageDataType>("user_status",
            StorageDataType.UserStatus(
                loginStatus = response.loginStatus,
                pinRequired = (if (response.loginStatus == "registerRequired") response.pinRequired else false),
            )
        )

        if (response.loginStatus != "pinRequired" && response.walletId != null) {
            setStorage("wallet_id", response.walletId)
            setStorage<StorageDataType>("user_info",
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
        } else {
            val userInfo = StorageDataType.UserInfo(
                status = "success",
                userInfo = UserInfoDetails(
                    userId = response.userInfo.userId,
                    email = response.userInfo.email,
                    provider = request.provider.value,
                    use2FA = (response.userInfo.use2FA >= 2),
                ),
            )
            setStorage<StorageDataType>("user_info", userInfo)
        }
        setStorage("oauth_provider_pending", request.provider.value)
    }

    private fun _prevStorageReadAll(): Map<String, Any?>? {
        return try {
            _prevStorage.all
                .filterKeys { it.startsWith(_appId) }
                .mapKeys { it.key.removePrefix("${_appId}_") }
        } catch (e: Exception) {
            _prevDeleteAll()
            null
        }
    }

    private fun _prevDeleteAll() {
        try {
            _prevStorage.edit().clear().apply()
        } catch(e: Exception) {
            return
        }
    }
}
