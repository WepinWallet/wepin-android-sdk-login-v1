package com.wepin.android.loginlib

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wepin.android.loginlib.const.RegExpConst
import com.wepin.android.loginlib.error.WepinError
import com.wepin.android.loginlib.manager.WepinLoginManager
import com.wepin.android.loginlib.network.WepinNetworkManager
import com.wepin.android.loginlib.storage.StorageManager
import com.wepin.android.loginlib.types.FBToken
import com.wepin.android.loginlib.types.LoginOauth2Params
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.LoginWithEmailParams
import com.wepin.android.loginlib.types.Providers
import com.wepin.android.loginlib.types.StorageDataType
import com.wepin.android.loginlib.types.WepinLoginOptions
import com.wepin.android.loginlib.types.WepinUser
import com.wepin.android.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.android.loginlib.types.network.LoginOauthIdTokenRequest
import com.wepin.android.loginlib.types.network.firebase.GetRefreshIdTokenRequest
import com.wepin.android.loginlib.utils.bigIntegerToByteArrayTrimmed
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bouncycastle.util.encoders.Hex
import java.util.concurrent.CompletableFuture


class WepinLogin(wepinLoginOptions: WepinLoginOptions) {
    private val TAG = this.javaClass.name
    private var _contex: Context? = wepinLoginOptions.context
    private var _isInitialized = false
    private var _appId: String? = wepinLoginOptions.appId
    private var _appKey: String? = wepinLoginOptions.appKey
    private var _wepinLoginMangager: WepinLoginManager = WepinLoginManager.getInstance()
    private var _wepinNewtorkManager: WepinNetworkManager? = null

    // CoroutineScope 생성 (IO 디스패처 사용)
    // Job 인스턴스 생성
//    private val repositoryJob = Job()
//    private val repositoryScope = CoroutineScope(Dispatchers.IO + repositoryJob)

    fun init() : CompletableFuture<Boolean>? {
        val wepinCompletableFutre: CompletableFuture<Boolean> = CompletableFuture<Boolean>()
        if(_isInitialized){
            wepinCompletableFutre.completeExceptionally(
                WepinError.ALREADY_INITIALIZED_ERROR
            )
            return wepinCompletableFutre
        }
        if(_contex == null || _contex !is Activity) {
            wepinCompletableFutre.completeExceptionally(
                WepinError.NOT_ACTIVITY
            )
            return wepinCompletableFutre
        }

        _wepinLoginMangager.init(_contex as Activity, _appKey!!, _appId!!)
        _wepinNewtorkManager = _wepinLoginMangager.wepinNewtorkManager
        _wepinNewtorkManager!!.getFirebaseConfig().thenApply { configResponse ->
            _wepinLoginMangager.setFirebase(configResponse as String)
        }
        StorageManager.init(_contex as Activity, _appId!!)
        StorageManager.deleteAllIfAppIdDataNotExists()
        checkExistWepinLoginSession().thenApply {}

        return _wepinNewtorkManager?.getAppInfo()
            ?.thenApply { infoResponse ->
            Log.d(TAG, "infoResponse $infoResponse")
            _isInitialized = infoResponse !== null
            _isInitialized
        }
    }

    private fun checkExistWepinLoginSession() : CompletableFuture<Boolean> {
        val wepinCompletableFuture: CompletableFuture<Boolean> = CompletableFuture<Boolean>()
        val token = StorageManager.getStorage("wepin:connectUser")
        val userId = StorageManager.getStorage("user_id")
        if (token != null && userId != null){
            _wepinNewtorkManager?.setAuthToken((token as StorageDataType.WepinToken).accessToken, (token as StorageDataType.WepinToken).refreshToken)
            _wepinNewtorkManager?.getAccessToken(userId = userId as String) ?.thenApply { response ->
                Log.d(TAG, "checkExistWepinLoginSession response $response")
                StorageManager.setStorage(
                    "wepin:connectUser",
                    StorageDataType.WepinToken(
                        accessToken = response,
                        refreshToken = (token as StorageDataType.WepinToken).refreshToken,
                    )
                )
                _wepinNewtorkManager?.setAuthToken(response, (token as StorageDataType.WepinToken).refreshToken)
                wepinCompletableFuture.complete(true)
                wepinCompletableFuture
            }?.exceptionally {
                _wepinNewtorkManager?.clearAuthToken()
                StorageManager.deleteAllStorageWithAppId()
                wepinCompletableFuture.complete(true)
                wepinCompletableFuture
            }
        }else{
            _wepinNewtorkManager?.clearAuthToken()
            StorageManager.deleteAllStorageWithAppId()
            wepinCompletableFuture.complete(true)
        }
        return wepinCompletableFuture
    }
    fun finalize() {
        StorageManager.deleteAllStorage()
        _isInitialized = false
    }

    fun isInitialized() :Boolean {
        return _isInitialized
    }

    fun loginWithOauthProvider(params: LoginOauth2Params): CompletableFuture<LoginOauthResult> {
        _wepinLoginMangager.initLoginCompletableFuture()
        if(!_isInitialized){
            _wepinLoginMangager.loginOauthCompletableFuture.completeExceptionally(
                WepinError.NOT_INITIALIZED_ERROR
            )
            return _wepinLoginMangager.loginOauthCompletableFuture
        }

        if(_contex === null || _contex !is Activity) {
            _wepinLoginMangager.loginOauthCompletableFuture.completeExceptionally(
                WepinError.NOT_ACTIVITY
            )
            return _wepinLoginMangager.loginOauthCompletableFuture
        }

        if(!WepinNetworkManager.isInternetAvailable(_contex as Activity)) {
            _wepinLoginMangager.loginOauthCompletableFuture.completeExceptionally(
                WepinError.NOT_CONNECTED_INTERNET
            )
            return _wepinLoginMangager.loginOauthCompletableFuture
        }

        if(Providers.isNotCommonProvider(params.provider)){
            _wepinLoginMangager.loginOauthCompletableFuture.completeExceptionally(
                WepinError.INVALID_LOGIN_PROVIDER
            )
            return _wepinLoginMangager.loginOauthCompletableFuture
        }

        val intent = Intent(_contex, WepinLoginMainActivity::class.java).apply {
            putExtra("provider", params.provider)
            putExtra("clientId", params.clientId)
        }
        (_contex as Activity).startActivity(intent)
        return _wepinLoginMangager.loginOauthCompletableFuture
    }

    fun loginWithIdToken(params: LoginOauthIdTokenRequest): CompletableFuture<LoginResult> {
        _wepinLoginMangager.initLoginCompletableFuture()
        if(!_isInitialized){
            _wepinLoginMangager.loginCompletableFuture.completeExceptionally(
                WepinError.NOT_INITIALIZED_ERROR
            )
            return _wepinLoginMangager.loginCompletableFuture
        }
        StorageManager.deleteAllStorage()
        return _wepinNewtorkManager?.loginOAuthIdToken(params)?.thenCompose  { loginResponse ->
            if(loginResponse.token != null) _wepinLoginMangager.loginHelper?.doFirebaseLoginWithCustomToken(loginResponse.token, Providers.EXTERNAL_TOKEN)
            else {
                val failedFuture = CompletableFuture<LoginResult>()
                failedFuture.completeExceptionally(WepinError.INVALID_TOKEN)
                failedFuture
            }
        } ?: CompletableFuture<LoginResult>().apply {
            completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
        }
    }

    fun loginWithAccessToken(params: LoginOauthAccessTokenRequest): CompletableFuture<LoginResult> {
        _wepinLoginMangager.initLoginCompletableFuture()
        if(!_isInitialized){
            _wepinLoginMangager.loginCompletableFuture.completeExceptionally(
                WepinError.NOT_INITIALIZED_ERROR
            )
            return _wepinLoginMangager.loginCompletableFuture
        }
        if(Providers.isNotAccessTokenProvider(params.provider)){
            _wepinLoginMangager.loginCompletableFuture.completeExceptionally(
                WepinError.INVALID_LOGIN_PROVIDER
            )
            return _wepinLoginMangager.loginCompletableFuture
        }
        StorageManager.deleteAllStorage()
        return _wepinNewtorkManager?.loginOAuthAccessToken(params)?.thenCompose  { loginResponse ->
            if(loginResponse.token != null) _wepinLoginMangager.loginHelper?.doFirebaseLoginWithCustomToken(loginResponse.token, Providers.EXTERNAL_TOKEN)
            else {
                val failedFuture = CompletableFuture<LoginResult>()
                failedFuture.completeExceptionally(WepinError.INVALID_TOKEN)
                failedFuture
            }
        } ?: CompletableFuture<LoginResult>().apply {
            completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
        }
    }

    fun signUpWithEmailAndPassword(params: LoginWithEmailParams): CompletableFuture<LoginResult> {
        _wepinLoginMangager.initLoginCompletableFuture()
        if(!_isInitialized){
            _wepinLoginMangager.loginCompletableFuture.completeExceptionally(
                WepinError.NOT_INITIALIZED_ERROR
            )
            return _wepinLoginMangager.loginCompletableFuture
        }
        if(params.email.isEmpty() ||!RegExpConst.validateEmail(email = params.email)){
            _wepinLoginMangager.loginCompletableFuture.completeExceptionally(
                WepinError.INCORRECT_EMAIL_FORM
            )
            return _wepinLoginMangager.loginCompletableFuture
        }

        if(params.password.isEmpty() || !RegExpConst.validatePassword(password = params.password)){
            _wepinLoginMangager.loginCompletableFuture.completeExceptionally(
                WepinError.INCORRECT_PASSWORD_FORM
            )
            return _wepinLoginMangager.loginCompletableFuture
        }

        return _wepinNewtorkManager?.checkEmailExist(params.email)
            ?.thenCompose  { checkEmailResponse ->
                if (checkEmailResponse.isEmailExist && checkEmailResponse.isEmailverified && checkEmailResponse.providerIds.contains(
                        "password"
                    )) {
                    _wepinLoginMangager.loginCompletableFuture.completeExceptionally(WepinError.EXISTED_EMAIL)
                    _wepinLoginMangager.loginCompletableFuture
                } else {
                    _wepinLoginMangager.loginHelper?.verifySignUpFirebase(params)
                }
            } ?: CompletableFuture<LoginResult>().apply {
            completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
        }
    }


    fun loginWithEmailAndPassword(params: LoginWithEmailParams): CompletableFuture<LoginResult> {
        _wepinLoginMangager.initLoginCompletableFuture()
        if(!_isInitialized){
            _wepinLoginMangager.loginCompletableFuture.completeExceptionally(
                WepinError.NOT_INITIALIZED_ERROR
            )
            return _wepinLoginMangager.loginCompletableFuture
        }

        if (params.email.isEmpty() || !RegExpConst.validateEmail(email = params.email)) {
            _wepinLoginMangager.loginCompletableFuture.completeExceptionally(
                WepinError.INCORRECT_EMAIL_FORM
            )
            return _wepinLoginMangager.loginCompletableFuture
        }

        if (params.password.isEmpty() || !RegExpConst.validatePassword(password = params.password)) {
            _wepinLoginMangager.loginCompletableFuture.completeExceptionally(
                WepinError.INCORRECT_PASSWORD_FORM
            )
            return _wepinLoginMangager.loginCompletableFuture
        }

//      Java 호환성을 위해 코루틴 대신 CompletableFuture을 사용하자!
//        val checkEmailExistResponse = withContext(Dispatchers.IO) {
//            _wepinNewtorkManager?.checkEmailExist(params.email)
//        } ?: throw WepinError("required/signup-email")
//
//        val (isEmailExist, isEmailVerified, providerIds) = checkEmailExistResponse
//        // 계정이 있는 경우
//        if (isEmailExist && isEmailVerified && providerIds.contains("password")) {
//            return loginWithEmailAndResetPasswordState(params.email, params.password)
//        } else {
//            throw WepinError("required/signup-email")
//        }

//        _wepinLoginMangager.loginCompletableFuture = CompletableFuture<LoginResult>()
         return _wepinNewtorkManager?.checkEmailExist(params.email)?.thenCompose  { checkEmailResponse ->
             if (checkEmailResponse.isEmailExist && checkEmailResponse.isEmailverified && checkEmailResponse.providerIds.contains(
                     "password"
                 )) {
                 _wepinLoginMangager.loginHelper?.loginWithEmailAndResetPasswordState(params.email, params.password)
             } else {
                 val failedFuture = CompletableFuture<LoginResult>()
                 failedFuture.completeExceptionally(WepinError.REQUIRED_SIGNUP_EMAIL)
                 failedFuture
             }

        } ?: CompletableFuture<LoginResult>().apply {
            completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
        }
    }

    fun getRefreshFirebaseToken(): CompletableFuture<LoginResult> {
        _wepinLoginMangager.initLoginCompletableFuture()
        if(!_isInitialized){
            _wepinLoginMangager.loginWepinCompletableFutre.completeExceptionally(
                WepinError.NOT_INITIALIZED_ERROR
            )
            return _wepinLoginMangager.loginCompletableFuture
        }
        val firebaseToken = StorageManager.getStorage("firebase:wepin")

        if(firebaseToken != null){
            val provider = (firebaseToken as StorageDataType.FirebaseWepin).provider
            val refreshToken = (firebaseToken as StorageDataType.FirebaseWepin).refreshToken
            _wepinLoginMangager.wepinFirebaseManager?.getRefreshIdToken(GetRefreshIdTokenRequest(refreshToken))?.whenComplete { response, error ->
                if (error != null) {
                    _wepinLoginMangager.loginCompletableFuture.completeExceptionally(WepinError("${error.message}"))
                }else {
                    val token = FBToken(
                        idToken = response.id_token,
                        refreshToken = response.refresh_token
                    )
                    val loginResult = LoginResult(
                        provider = Providers.fromValue(
                            provider
                        )!!, token = token
                    )
                    StorageManager.setFirebaseUser(loginResult);
                    _wepinLoginMangager.loginCompletableFuture.complete(loginResult)

                }
            }
        }else{
            _wepinLoginMangager.loginCompletableFuture.completeExceptionally(
                WepinError.INVALID_LOGIN_SESSION
            )
        }

        return _wepinLoginMangager.loginCompletableFuture
    }
    fun getCurrentWepinUser(): CompletableFuture<WepinUser> {
        _wepinLoginMangager.initLoginCompletableFuture()
        if(!_isInitialized){
            _wepinLoginMangager.loginWepinCompletableFutre.completeExceptionally(
                WepinError.NOT_INITIALIZED_ERROR
            )
            return _wepinLoginMangager.loginWepinCompletableFutre
        }
        checkExistWepinLoginSession().thenApply {}
        val wepinUser = StorageManager.getWepinUser()
        if(wepinUser != null) {
            _wepinLoginMangager.loginWepinCompletableFutre.complete(wepinUser)
        }else {
            _wepinLoginMangager.loginWepinCompletableFutre.completeExceptionally(
                WepinError.INVALID_LOGIN_SESSION
            )
        }
        return _wepinLoginMangager.loginWepinCompletableFutre
    }
    fun loginWepin(params: LoginResult): CompletableFuture<WepinUser> {
        _wepinLoginMangager.initLoginCompletableFuture()
        if(!_isInitialized){
            _wepinLoginMangager.loginWepinCompletableFutre.completeExceptionally(
                WepinError.NOT_INITIALIZED_ERROR
            )
            return _wepinLoginMangager.loginWepinCompletableFutre
        }
        if (params.token.idToken.isEmpty() || params.token.refreshToken.isEmpty()) {
            _wepinLoginMangager.loginWepinCompletableFutre.completeExceptionally(
                WepinError.INVALID_PARAMETER
            )
            return _wepinLoginMangager.loginWepinCompletableFutre
        }

        _wepinLoginMangager.wepinNewtorkManager?.login(params.token.idToken)
            ?.whenComplete { loginResponse, error ->
                if(error != null){
                    _wepinLoginMangager.loginWepinCompletableFutre.completeExceptionally(WepinError("${error.message}"))
                }else {
                    StorageManager.setWepinUser(params, loginResponse)
                    val wepinUser = StorageManager.getWepinUser()
                    _wepinLoginMangager.loginWepinCompletableFutre.complete(wepinUser)
                }
            }

        return _wepinLoginMangager.loginWepinCompletableFutre
    }

    fun logoutWepin() : CompletableFuture<Boolean> {
        val wepinCompletableFutre: CompletableFuture<Boolean> = CompletableFuture<Boolean>()
        if(!_isInitialized){
            wepinCompletableFutre.completeExceptionally(
                WepinError.NOT_INITIALIZED_ERROR
            )
            return wepinCompletableFutre
        }

        val userId = StorageManager.getStorage("user_id")
        if (userId == null){
            wepinCompletableFutre.completeExceptionally(
                WepinError.ALREADY_LOGOUT
            )
            return wepinCompletableFutre
        }

        _wepinLoginMangager.wepinNewtorkManager?.logout(userId as String)
            ?.whenComplete { response, error ->
                if (error != null) {
                    wepinCompletableFutre.completeExceptionally(
                        WepinError(
                            "${error.message}"
                        )
                    )
                } else {
                    StorageManager.deleteAllStorage()
                    wepinCompletableFutre.complete(response)
                }
            }
        return wepinCompletableFutre
    }

    fun getSignForLogin(privateKeyHex: String, message: String): String {
        // 새로운 ECKey 생성
        val ecKey = ECKey.fromPrivate(Hex.decode(privateKeyHex))

        // 메시지의 SHA-256 해시 생성
        val sha256Hash = Sha256Hash.of(message.toByteArray())
        // 생성된 해시에 대한 서명 생성
        val ecdsaSignature = ecKey.sign(sha256Hash)

        // 서명을 DER 형식으로 변환
//            val derSignature = ecdsaSignature.encodeToDER()
        // 서명을 Hex 문자열로 변환 (옵션)
        val rhexSignature = Hex.toHexString(bigIntegerToByteArrayTrimmed(ecdsaSignature.r))
        val shexSignature = Hex.toHexString(bigIntegerToByteArrayTrimmed(ecdsaSignature.s))

        return rhexSignature + shexSignature
    }

}