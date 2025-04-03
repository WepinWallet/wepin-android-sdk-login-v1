package com.wepin.android.loginlib

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.commonlib.types.LoginOauthAccessTokenRequest
import com.wepin.android.commonlib.types.LoginOauthIdTokenRequest
import com.wepin.android.commonlib.types.Providers
import com.wepin.android.commonlib.types.WepinUser
import com.wepin.android.loginlib.manager.WepinLoginManager
import com.wepin.android.loginlib.manager.WepinLoginStorageManager
import com.wepin.android.loginlib.types.FBToken
import com.wepin.android.loginlib.types.LoginOauth2Params
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.LoginWithEmailParams
import com.wepin.android.loginlib.types.WepinLoginOptions
import com.wepin.android.loginlib.utils.getVersionMetaDataValue
import com.wepin.android.networklib.WepinFirebase
import com.wepin.android.networklib.WepinNetwork
import com.wepin.android.networklib.types.wepin.OAuthProviderInfo
import com.wepin.android.networklib.types.wepin.WepinRegex
import com.wepin.android.sessionlib.WepinSessionManager
import com.wepin.android.storage.WepinStorageManager
import com.wepin.android.storage.types.StorageDataType
import java.lang.ref.WeakReference
import java.util.concurrent.CompletableFuture

class WepinLogin(wepinLoginOptions: WepinLoginOptions, platformType: String? = "android") {
    private val TAG = this.javaClass.name
    private var _isInitialized = false
    private val contextRef: WeakReference<Context> = WeakReference(wepinLoginOptions.context)
    private val _appKey: String = wepinLoginOptions.appKey
    private val _appId: String = wepinLoginOptions.appId
    private val _platformType: String = platformType ?: "android"
    private val _wepinLoginManager: WepinLoginManager = WepinLoginManager.getInstance()
    private var _wepinSessionManager: WepinSessionManager? = null
    private var _wepinNetwork: WepinNetwork? = null
    private var _providerInfo: Array<OAuthProviderInfo>? = null
    private var _regex: WepinRegex? = null
    val version: String = getVersionMetaDataValue()

    fun init(): CompletableFuture<Boolean> {
        Log.d(TAG, "init")
        val wepinCompletableFuture: CompletableFuture<Boolean> = CompletableFuture()

        try {
            if (_isInitialized) {
                wepinCompletableFuture.completeExceptionally(WepinError.ALREADY_INITIALIZED_ERROR)
                return wepinCompletableFuture
            }

            val context = contextRef.get() ?: run {
                wepinCompletableFuture.completeExceptionally(WepinError.generalUnKnownEx("Invalid Context"))
                return wepinCompletableFuture
            }

            if (context !is Activity) {
                wepinCompletableFuture.completeExceptionally(WepinError.NOT_ACTIVITY)
                return wepinCompletableFuture
            }

            // Step 1: Initialize WepinNetwork
            WepinNetwork.initialize(
                context,
                _appKey,
                context.packageName,
                "$_platformType-login",
                version
            )
                .thenCompose { network ->
                    _wepinNetwork = network
                    Log.d(TAG, "WepinNetwork initialized")

                    // Step 2: Get Firebase Config
                    network.getFirebaseConfig()
                }
                .thenCompose { configRes ->
                    // Step 3: Initialize WepinFirebase
                    WepinFirebase.initialize(context, configRes)
                }
                .thenAccept { firebase ->
                    Log.d(TAG, "WepinFirebase initialized")

                    // Step 4: Initialize LoginManager
                    _wepinLoginManager.init(_appKey, _appId)

                    // Step 5: Initialize SessionManager
                    WepinSessionManager.initialize()
                    _wepinSessionManager = WepinSessionManager.getInstance()
                    _wepinSessionManager?.setNetworkAndFirebase(_wepinNetwork!!, firebase)
                    Log.d(TAG, "SessionManager initialized")

                    // Step 6: Initialize StorageManager
                    WepinStorageManager.init(context, _appId)
                    Log.d(TAG, "StorageManager initialized")

                    // Step 7: Get provider info, regex, and app info
                    val providerFuture = _wepinNetwork?.getOAuthProviderInfo()
                    val regexFuture = _wepinNetwork?.getRegex()
                    val appInfoFuture = _wepinNetwork?.getAppInfo()

                    CompletableFuture.allOf(providerFuture, regexFuture, appInfoFuture)
                        .thenApply {
                            _providerInfo = providerFuture?.get()
                            _regex = regexFuture?.get()
                            val infoResponse = appInfoFuture?.get()
                            _isInitialized = infoResponse !== null
                            wepinCompletableFuture.complete(_isInitialized)
                        }
                        .exceptionally { error ->
                            _isInitialized = false
                            wepinCompletableFuture.completeExceptionally(error)
                            null
                        }
                }
                .exceptionally { error ->
                    finalize()
                    wepinCompletableFuture.completeExceptionally(WepinError.NOT_INITIALIZED_ERROR)
                    null
                }
        } catch (e: Exception) {
            finalize()
            wepinCompletableFuture.completeExceptionally(WepinError.NOT_INITIALIZED_ERROR)
        }
        return wepinCompletableFuture
    }

    fun isInitialized(): Boolean = _isInitialized

    fun loginWithOauthProvider(params: LoginOauth2Params): CompletableFuture<LoginOauthResult> {
        try {
            validateLoginRequest(true)
            _wepinLoginManager.initCompletableFuture()

            val provider = _providerInfo?.find { it.isSupportProvider(params.provider) }
                ?: return CompletableFuture<LoginOauthResult>().apply {
                    completeExceptionally(WepinError.INVALID_LOGIN_PROVIDER)
                }

            val context = contextRef.get() ?: run {
                return CompletableFuture<LoginOauthResult>().apply {
                    completeExceptionally(WepinError.generalUnKnownEx("Invalid Context"))
                }
            }
            val intent = Intent(context, WepinLoginMainActivity::class.java).apply {
                putExtra("providerInfo", provider)
                putExtra("clientId", params.clientId)
            }

            (context as Activity).startActivity(intent)

            return _wepinLoginManager.getLoginOAuthFuture()
        } catch (e: Exception) {
            return CompletableFuture<LoginOauthResult>().apply {
                completeExceptionally(e)
            }
        }
    }

    fun signUpWithEmailAndPassword(params: LoginWithEmailParams): CompletableFuture<LoginResult> {
        try {
            validateLoginRequest(false)
        } catch (error: Exception) {
            return CompletableFuture<LoginResult>().apply {
                completeExceptionally(error)
            }
        }
        _wepinLoginManager.initCompletableFuture()
        if (params.email.isEmpty() || !_regex!!.validateEmail(params.email)) {
            _wepinLoginManager.loginHelper?.handleLoginError(WepinError.INCORRECT_EMAIL_FORM)
            return _wepinLoginManager.getLoginFuture()
        }
        if (params.password.isEmpty() || !_regex!!.validatePassword(params.password)) {
            _wepinLoginManager.loginHelper?.handleLoginError(WepinError.INCORRECT_PASSWORD_FORM)
            return _wepinLoginManager.getLoginFuture()
        }

        return _wepinNetwork?.checkEmailExist(params.email)
            ?.thenCompose { checkEmailResponse ->
                if (checkEmailResponse.isEmailExist
                    && checkEmailResponse.isEmailverified
                    && checkEmailResponse.providerIds.contains("password")
                ) {
                    _wepinLoginManager.loginHelper?.handleLoginError(WepinError.EXISTED_EMAIL)

                } else {
                    _wepinLoginManager.loginHelper?.verifySignUpFirebase(params)
                }
                _wepinLoginManager.getLoginFuture()
            } ?: CompletableFuture<LoginResult>().apply {
            completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
        }
    }

    fun loginWithEmailAndPassword(params: LoginWithEmailParams): CompletableFuture<LoginResult> {
        try {
            validateLoginRequest(false)
        } catch (error: Exception) {
            return CompletableFuture<LoginResult>().apply {
                completeExceptionally(error)
            }
        }
        _wepinLoginManager.initCompletableFuture()

        if (params.email.isEmpty() || !_regex!!.validateEmail(params.email)) {
            _wepinLoginManager.loginHelper?.handleLoginError(WepinError.INCORRECT_EMAIL_FORM)
            return _wepinLoginManager.getLoginFuture()
        }

        if (params.password.isEmpty() || !_regex!!.validatePassword(params.password)) {
            _wepinLoginManager.loginHelper?.handleLoginError(WepinError.INCORRECT_PASSWORD_FORM)
            return _wepinLoginManager.getLoginFuture()
        }

        return _wepinNetwork?.checkEmailExist(params.email)?.thenCompose { checkEmailResponse ->
            if (checkEmailResponse.isEmailExist && checkEmailResponse.isEmailverified && checkEmailResponse.providerIds.contains(
                    "password"
                )
            ) {
                _wepinLoginManager.loginHelper?.loginWithEmailAndResetPasswordState(
                    params.email,
                    params.password
                )
            } else {
                _wepinLoginManager.loginHelper?.handleLoginError(WepinError.REQUIRED_SIGNUP_EMAIL)
            }
            _wepinLoginManager.getLoginFuture()
        } ?: CompletableFuture<LoginResult>().apply {
            completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
        }
    }

    fun loginWithIdToken(params: LoginOauthIdTokenRequest): CompletableFuture<LoginResult> {
        try {
            validateLoginRequest(false)
        } catch (error: Exception) {
            return CompletableFuture<LoginResult>().apply {
                completeExceptionally(error)
            }
        }
        _wepinLoginManager.initCompletableFuture()

        WepinStorageManager.deleteAllStorage()
        return _wepinNetwork?.loginOAuthIdToken(params)?.thenCompose { loginResponse ->
            if (loginResponse.token != null) {
                _wepinLoginManager.loginHelper?.doFirebaseLoginWithCustomToken(
                    loginResponse.token!!,
                    "external_token"
                )
            } else {
                _wepinLoginManager.loginHelper?.handleLoginError(WepinError.INVALID_TOKEN)
            }
            _wepinLoginManager.getLoginFuture()
        } ?: CompletableFuture<LoginResult>().apply {
            completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
        }
    }

    fun loginWithAccessToken(params: LoginOauthAccessTokenRequest): CompletableFuture<LoginResult> {
        try {
            validateLoginRequest(false)
        } catch (error: Exception) {
            return CompletableFuture<LoginResult>().apply {
                completeExceptionally(error)
            }
        }
        _wepinLoginManager.initCompletableFuture()

        val provider = _providerInfo?.find { provider ->
            provider.provider == params.provider && !provider.supportIdToken()
        }
        if (provider == null) {
            _wepinLoginManager.loginHelper?.handleLoginError(WepinError.INVALID_LOGIN_PROVIDER)
            return _wepinLoginManager.getLoginFuture()
        }

        WepinStorageManager.deleteAllStorage()
        return _wepinNetwork?.loginOAuthAccessToken(params)?.thenCompose { loginResponse ->
            if (loginResponse.token != null) {
                _wepinLoginManager.loginHelper?.doFirebaseLoginWithCustomToken(
                    loginResponse.token!!,
                    "external_token"
                )
            } else {
                _wepinLoginManager.loginHelper?.handleLoginError(WepinError.INVALID_TOKEN)
            }
            _wepinLoginManager.getLoginFuture()
        } ?: CompletableFuture<LoginResult>().apply {
            completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
        }
    }

    fun getRefreshFirebaseToken(prevFBToken: LoginResult? = null): CompletableFuture<LoginResult> {
        try {
            validateLoginRequest(false)
        } catch (error: Exception) {
            return CompletableFuture<LoginResult>().apply {
                completeExceptionally(error)
            }
        }
        _wepinLoginManager.initCompletableFuture()

        if (prevFBToken != null) {
            _wepinLoginManager.loginHelper?.getRefreshIdToken(
                prevFBToken.provider,
                prevFBToken.token.refreshToken
            )

            return _wepinLoginManager.getLoginFuture()
        }
        val sessionFuture = _wepinSessionManager?.checkExistFirebaseLoginSession()

        if (sessionFuture == null) {
            _wepinLoginManager.loginHelper?.handleLoginError(WepinError.INVALID_LOGIN_SESSION)
            return _wepinLoginManager.getLoginFuture()
        }

        sessionFuture.thenAccept { result ->
            if (result) {
                val token =
                    WepinStorageManager.getStorage<StorageDataType.FirebaseWepin>("firebase:wepin")
                if (token == null) {
                    _wepinLoginManager.loginHelper?.handleLoginError(WepinError.INVALID_LOGIN_SESSION)
                } else {
                    _wepinLoginManager.loginHelper?.handleLoginResult(
                        LoginResult(
                            provider = Providers.fromValue(token.provider)!!,
                            token = FBToken(
                                token.idToken,
                                refreshToken = token.refreshToken
                            )
                        )
                    )
                }
            } else {
                _wepinLoginManager.loginHelper?.handleLoginError(WepinError.INVALID_LOGIN_SESSION)
            }
        }
        return _wepinLoginManager.getLoginFuture()
    }

    fun loginWepin(params: LoginResult): CompletableFuture<WepinUser> {
        try {
            validateLoginRequest(false)
        } catch (error: Exception) {
            return CompletableFuture<WepinUser>().apply {
                completeExceptionally(error)
            }
        }
        _wepinLoginManager.initCompletableFuture()

        if (params.token.idToken.isEmpty() || params.token.refreshToken.isEmpty()) {
            _wepinLoginManager.loginHelper?.handleLoginWepinError(WepinError.INVALID_PARAMETER)
            return _wepinLoginManager.getWepinUserFuture()
        }

        _wepinLoginManager.wepinNetwork?.login(params.token.idToken)
            ?.whenComplete { loginResponse, error ->
                if (error != null) {
                    _wepinLoginManager.loginHelper?.handleLoginWepinError(WepinError("${error.message}"))
                } else {
                    WepinLoginStorageManager.setWepinUser(
                        params,
                        loginResponse
                    )
                    val wepinUser = WepinLoginStorageManager.getWepinUser()

                    if (wepinUser != null) {
                        _wepinLoginManager.loginHelper?.handleLoginWepinResult(wepinUser)
                    } else {
                        _wepinLoginManager.loginHelper?.handleLoginWepinError(WepinError.FAILED_LOGIN)
                    }
                }
            }
        return _wepinLoginManager.getWepinUserFuture()
    }

    fun getCurrentWepinUser(): CompletableFuture<WepinUser> {
        Log.d(TAG, "getCurrentWepinUser")
        try {
            validateLoginRequest(false)
        } catch (error: Exception) {
            return CompletableFuture<WepinUser>().apply {
                completeExceptionally(error)
            }
        }
        _wepinLoginManager.initCompletableFuture()

        try {
            _wepinSessionManager?.checkLoginStatusAndGetLifeCycle()?.thenApply {}

            val wepinUser = _wepinSessionManager?.getWepinUser()
            if (wepinUser != null) {
                _wepinLoginManager.loginHelper?.handleLoginWepinResult(wepinUser)
            } else {
                _wepinLoginManager.loginHelper?.handleLoginWepinError(WepinError.INVALID_LOGIN_SESSION)
            }
            return _wepinLoginManager.getWepinUserFuture()
        } catch (error: Exception) {
            if (error is WepinError) _wepinLoginManager.loginHelper?.handleLoginWepinError(error)
            else _wepinLoginManager.loginHelper?.handleLoginWepinError(WepinError.INVALID_LOGIN_SESSION)
            return _wepinLoginManager.getWepinUserFuture()
        }
    }

    fun logoutWepin(): CompletableFuture<Boolean> {
        try {
            validateLoginRequest(false)
        } catch (error: Exception) {
            return CompletableFuture<Boolean>().apply {
                completeExceptionally(error)
            }
        }
        val wepinCompleteFuture: CompletableFuture<Boolean> = CompletableFuture<Boolean>()

        val userId = WepinStorageManager.getStorage<String>("user_id")
        if (userId == null) {
            wepinCompleteFuture.completeExceptionally(WepinError.ALREADY_LOGOUT)
            return wepinCompleteFuture
        }

        _wepinNetwork?.logout(userId)
            ?.whenComplete { response, error ->
                if (error != null) {
                    wepinCompleteFuture.completeExceptionally(WepinError("${error.message}"))
                } else {
                    WepinStorageManager.deleteAllStorage()
                    wepinCompleteFuture.complete(response)
                }
            }
        return wepinCompleteFuture
    }

    @Deprecated(
        message = "getSignForLogin() is no longer supported because the 'sign' parameter has been removed from the login process. To log in without a signature, please delete the Auth Key in your Wepin Workspace (Development Tools > Login tab > Auth Key > Delete). The Auth Key menu is visible only if a key was previously generated. Refer to the latest developer guide for more information."
    )
    fun getSignForLogin(privateKeyHex: String, message: String) {
        throw WepinError(
            WepinError.Companion.ErrorCode.DEPRECATED.ordinal,
            "getSignForLogin() is no longer supported because the 'sign' parameter has been removed from the login process. To log in without a signature, please delete the Auth Key in your Wepin Workspace (Development Tools > Login tab > Auth Key > Delete). The Auth Key menu is visible only if a key was previously generated. Refer to the latest developer guide for more information."
        )
    }

    private fun validateLoginRequest(withContext: Boolean) {
        Log.d(TAG, "validateLoginRequest")
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        val context = contextRef.get() ?: run {
            throw WepinError.generalUnKnownEx("Invalid Context")
        }
        Log.d(TAG, "it's initialized")
        if (withContext && context !is Activity) {
            throw WepinError.NOT_ACTIVITY
        }
        Log.d(TAG, "it's activity")
        if (!WepinNetwork.isInternetAvailable(context)) {
            throw WepinError.NOT_CONNECTED_INTERNET
        }
    }

    fun finalize() {
        _wepinSessionManager?.finalize()
        _wepinSessionManager = null
        _wepinLoginManager.clear()
        _isInitialized = false
    }
}