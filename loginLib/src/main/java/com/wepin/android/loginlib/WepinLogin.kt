package com.wepin.android.loginlib

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.commonlib.types.LoginOauthAccessTokenRequest
import com.wepin.android.commonlib.types.LoginOauthIdTokenRequest
import com.wepin.android.commonlib.types.Providers
import com.wepin.android.commonlib.types.WepinUser
import com.wepin.android.core.WepinCoreManager
import com.wepin.android.core.network.WepinNetwork
import com.wepin.android.core.session.WepinSessionManager
import com.wepin.android.core.storage.WepinStorageManager
import com.wepin.android.core.types.storage.StorageDataType
import com.wepin.android.core.types.wepin.OAuthProviderInfo
import com.wepin.android.core.types.wepin.WepinRegex
import com.wepin.android.core.utils.Log
import com.wepin.android.core.utils.getVersionMetaDataValue
import com.wepin.android.loginlib.manager.WepinLoginManager
import com.wepin.android.loginlib.manager.WepinLoginStorageManager
import com.wepin.android.loginlib.types.FBToken
import com.wepin.android.loginlib.types.LoginOauth2Params
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.LoginWithEmailParams
import com.wepin.android.loginlib.types.WepinLoginOptions
import java.util.concurrent.CompletableFuture

class WepinLogin(
    wepinLoginOptions: WepinLoginOptions,
    private var platformType: String? = "android"
) {
    private val TAG = this.javaClass.name
    private var _isInitialized = false

    private val context: Context = wepinLoginOptions.context
    private val _appKey: String = wepinLoginOptions.appKey
    private val _appId: String = wepinLoginOptions.appId
    private var _wepinLoginManager: WepinLoginManager? = null
    private var _wepinSessionManager: WepinSessionManager? = null
    private var _wepinNetwork: WepinNetwork? = null
    private var _providerInfo: Array<OAuthProviderInfo>? = null
    var regex: WepinRegex = WepinRegex(
        email = "[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?",
        password = "^(?=.*[a-zA-Z])(?=.*[0-9]).{8,128}\$",
        pin = "^\\d{6,8}\$"
    )
    val version: String = getVersionMetaDataValue()

    companion object {
        internal var temporaryLoginManager: WepinLoginManager? = null
    }

    fun init(): CompletableFuture<Boolean> {
        Log.d(TAG, "init")
        val wepinCompletableFuture: CompletableFuture<Boolean> = CompletableFuture()

        try {
            if (_isInitialized) {
                wepinCompletableFuture.completeExceptionally(WepinError.ALREADY_INITIALIZED_ERROR)
                return wepinCompletableFuture
            }

            if (context !is Activity) {
                wepinCompletableFuture.completeExceptionally(WepinError.NOT_ACTIVITY)
                return wepinCompletableFuture
            }

            _wepinLoginManager = WepinLoginManager()
            _wepinLoginManager!!.init(
                context = context,
                appKey = _appKey,
                appId = _appId,
                platform = platformType ?: "android"
            )
                .thenAccept {
                    _wepinNetwork = WepinCoreManager.getNetwork()
                    _wepinSessionManager = WepinCoreManager.getSession()

                    val providerFuture = _wepinNetwork?.getOAuthProviderInfo()
                    val regexFuture = _wepinNetwork?.getRegex()

                    CompletableFuture.allOf(providerFuture, regexFuture)
                        .thenApply {
                            _providerInfo = providerFuture?.get()
                            regex = regexFuture?.get()!!
                            _isInitialized = true
                            wepinCompletableFuture.complete(_isInitialized)
                        }
                        .exceptionally { error ->
                            WepinCoreManager.clear()
                            _wepinNetwork = null
                            _wepinSessionManager = null
                            _wepinLoginManager = null
                            _isInitialized = false
                            wepinCompletableFuture.completeExceptionally(error)
                            null
                        }
                }
                .exceptionally { error ->
                    WepinCoreManager.clear()
                    _wepinNetwork = null
                    _wepinSessionManager = null
                    _wepinLoginManager = null
                    _isInitialized = false
                    val actualError = if (error.cause is WepinError) {
                        error.cause
                    } else {
                        WepinError.NOT_INITIALIZED_ERROR
                    }
                    wepinCompletableFuture.completeExceptionally(actualError)
                    null
                }
        } catch (e: Exception) {
            WepinCoreManager.clear()
            _wepinNetwork = null
            _wepinSessionManager = null
            _wepinLoginManager = null
            _isInitialized = false
            val actualError = if (e.cause is WepinError) {
                e.cause
            } else {
                WepinError.NOT_INITIALIZED_ERROR
            }
            wepinCompletableFuture.completeExceptionally(actualError)
        }
        return wepinCompletableFuture
    }

    fun isInitialized(): Boolean = _isInitialized

    fun loginWithOauthProvider(params: LoginOauth2Params): CompletableFuture<LoginOauthResult> {
        try {
            validateLoginRequest(true)
            _wepinLoginManager?.initCompletableFuture()

            val provider = _providerInfo?.find { it.isSupportProvider(params.provider) }
                ?: return CompletableFuture<LoginOauthResult>().apply {
                    completeExceptionally(WepinError.INVALID_LOGIN_PROVIDER)
                }

            temporaryLoginManager = _wepinLoginManager
            val intent = Intent(context, WepinLoginMainActivity::class.java).apply {
                putExtra("providerInfo", provider)
                putExtra("clientId", params.clientId)
            }

            (context as Activity).startActivity(intent)

            return _wepinLoginManager?.getLoginOAuthFuture()
                ?: CompletableFuture<LoginOauthResult>().apply {
                    completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
                }
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
        _wepinLoginManager?.initCompletableFuture()
        if (params.email.isEmpty() || !regex.validateEmail(params.email)) {
            _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.INCORRECT_EMAIL_FORM)
            return _wepinLoginManager?.getLoginFuture() ?: CompletableFuture<LoginResult>().apply {
                completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
            }
        }
        if (params.password.isEmpty() || !regex.validatePassword(params.password)) {
            _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.INCORRECT_PASSWORD_FORM)
            return _wepinLoginManager?.getLoginFuture() ?: CompletableFuture<LoginResult>().apply {
                completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
            }
        }

        return _wepinNetwork?.checkEmailExist(params.email)
            ?.thenCompose { checkEmailResponse ->
                if (checkEmailResponse.isEmailExist
                    && checkEmailResponse.isEmailverified
                    && checkEmailResponse.providerIds.contains("password")
                ) {
                    _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.EXISTED_EMAIL)

                } else {
                    _wepinLoginManager?.loginHelper?.verifySignUpFirebase(params)
                }
                _wepinLoginManager?.getLoginFuture()
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
        _wepinLoginManager?.initCompletableFuture()
        if (params.email.isEmpty() || !regex.validateEmail(params.email)) {
            _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.INCORRECT_EMAIL_FORM)
            return _wepinLoginManager?.getLoginFuture() ?: CompletableFuture<LoginResult>().apply {
                completeExceptionally(WepinError.INCORRECT_EMAIL_FORM)
            }
        }

        if (params.password.isEmpty() || !regex.validatePassword(params.password)) {
            _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.INCORRECT_PASSWORD_FORM)
            return _wepinLoginManager?.getLoginFuture() ?: CompletableFuture<LoginResult>().apply {
                completeExceptionally(WepinError.INCORRECT_PASSWORD_FORM)
            }
        }

        return _wepinNetwork?.checkEmailExist(params.email)?.thenCompose { checkEmailResponse ->
            if (checkEmailResponse.isEmailExist && checkEmailResponse.isEmailverified && checkEmailResponse.providerIds.contains(
                    "password"
                )
            ) {
                _wepinLoginManager?.loginHelper?.loginWithEmailAndResetPasswordState(
                    params.email,
                    params.password
                )
            } else {
                _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.REQUIRED_SIGNUP_EMAIL)
            }
            _wepinLoginManager?.getLoginFuture()
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
        _wepinLoginManager?.initCompletableFuture()

        WepinStorageManager.deleteAllStorage()
        return _wepinNetwork?.loginOAuthIdToken(params)?.whenComplete { loginResponse, throwable ->
            if (throwable == null) {
                if (loginResponse?.token != null) {
                    _wepinLoginManager?.loginHelper?.doFirebaseLoginWithCustomToken(
                        loginResponse.token!!,
                        "external_token"
                    )
                } else {
                    _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.INVALID_TOKEN)
                }
            } else {
                if (throwable.message?.contains("no_email") == true) {
                    _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.REQUIRED_SIGNUP_EMAIL)
                } else if (throwable is WepinError) {
                    _wepinLoginManager?.loginHelper?.handleLoginError(throwable)
                } else {
                    _wepinLoginManager?.loginHelper?.handleLoginError(
                        WepinError(
                            WepinError.FAILED_LOGIN.code,
                            throwable.message ?: "Login With IdToken failed"
                        )
                    )
                }
            }
        }?.let {
            _wepinLoginManager?.getLoginFuture()
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
        _wepinLoginManager?.initCompletableFuture()

        val provider = _providerInfo?.find { provider ->
            provider.provider == params.provider && !provider.supportIdToken()
        }
        if (provider == null) {
            _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.INVALID_LOGIN_PROVIDER)
            return _wepinLoginManager?.getLoginFuture() ?: CompletableFuture<LoginResult>().apply {
                completeExceptionally(WepinError.INVALID_LOGIN_PROVIDER)
            }
        }

        WepinStorageManager.deleteAllStorage()

        return _wepinNetwork?.loginOAuthAccessToken(params)
            ?.whenComplete { loginResponse, throwable ->
                if (throwable == null) {
                    if (loginResponse?.token != null) {
                        _wepinLoginManager?.loginHelper?.doFirebaseLoginWithCustomToken(
                            loginResponse.token!!,
                            "external_token"
                        )
                    } else {
                        _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.INVALID_TOKEN)
                    }
                } else {
                    if (throwable.message?.contains("no_email") == true) {
                        _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.REQUIRED_SIGNUP_EMAIL)
                    } else if (throwable is WepinError) {
                        _wepinLoginManager?.loginHelper?.handleLoginError(throwable)
                    } else {
                        _wepinLoginManager?.loginHelper?.handleLoginError(
                            WepinError(
                                WepinError.FAILED_LOGIN.code,
                                throwable.message ?: "Login With AccessToken failed"
                            )
                        )
                    }
                }
            }?.let {
                _wepinLoginManager?.getLoginFuture()
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
        _wepinLoginManager?.initCompletableFuture()

        if (prevFBToken != null) {
            _wepinLoginManager?.loginHelper?.getRefreshIdToken(
                prevFBToken.provider,
                prevFBToken.token.refreshToken
            )

            return _wepinLoginManager?.getLoginFuture() ?: CompletableFuture<LoginResult>().apply {
                completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
            }
        }
        val sessionFuture = _wepinSessionManager?.checkExistFirebaseLoginSession()

        if (sessionFuture == null) {
            _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.INVALID_LOGIN_SESSION)
            return _wepinLoginManager?.getLoginFuture() ?: CompletableFuture<LoginResult>().apply {
                completeExceptionally(WepinError.INVALID_LOGIN_SESSION)
            }
        }

        sessionFuture.thenAccept { result ->
            if (result) {
                val token =
                    WepinStorageManager.getStorage<StorageDataType.FirebaseWepin>("firebase:wepin")
                if (token == null) {
                    _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.INVALID_LOGIN_SESSION)
                } else {
                    _wepinLoginManager?.loginHelper?.handleLoginResult(
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
                _wepinLoginManager?.loginHelper?.handleLoginError(WepinError.INVALID_LOGIN_SESSION)
            }
        }
        return _wepinLoginManager?.getLoginFuture() ?: CompletableFuture<LoginResult>().apply {
            completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
        }
    }

    fun loginWepin(params: LoginResult): CompletableFuture<WepinUser> {
        try {
            validateLoginRequest(false)
        } catch (error: Exception) {
            return CompletableFuture<WepinUser>().apply {
                completeExceptionally(error)
            }
        }
        _wepinLoginManager?.initCompletableFuture()

        if (params.token.idToken.isEmpty() || params.token.refreshToken.isEmpty()) {
            _wepinLoginManager?.loginHelper?.handleLoginWepinError(WepinError.INVALID_PARAMETER)
            return _wepinLoginManager?.getWepinUserFuture()
                ?: CompletableFuture<WepinUser>().apply {
                    completeExceptionally(WepinError.INVALID_PARAMETER)
                }
        }

        _wepinLoginManager?.wepinNetwork?.login(params.token.idToken)
            ?.whenComplete { loginResponse, error ->
                if (error != null) {
                    _wepinLoginManager?.loginHelper?.handleLoginWepinError(WepinError("${error.message}"))
                } else {
                    WepinLoginStorageManager.setWepinUser(
                        params,
                        loginResponse
                    )
                    val wepinUser = WepinLoginStorageManager.getWepinUser()

                    if (wepinUser != null) {
                        _wepinLoginManager?.loginHelper?.handleLoginWepinResult(wepinUser)
                    } else {
                        _wepinLoginManager?.loginHelper?.handleLoginWepinError(WepinError.FAILED_LOGIN)
                    }
                }
            }
        return _wepinLoginManager?.getWepinUserFuture() ?: CompletableFuture<WepinUser>().apply {
            completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
        }
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
        _wepinLoginManager?.initCompletableFuture()

        try {
            _wepinSessionManager?.checkLoginStatusAndGetLifeCycle()?.thenApply {}

            val wepinUser = _wepinSessionManager?.getWepinUser()
            if (wepinUser != null) {
                _wepinLoginManager?.loginHelper?.handleLoginWepinResult(wepinUser)
            } else {
                _wepinLoginManager?.loginHelper?.handleLoginWepinError(WepinError.INVALID_LOGIN_SESSION)
            }
            return _wepinLoginManager?.getWepinUserFuture()
                ?: CompletableFuture<WepinUser>().apply {
                    completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
                }
        } catch (error: Exception) {
            if (error is WepinError) _wepinLoginManager?.loginHelper?.handleLoginWepinError(error)
            else _wepinLoginManager?.loginHelper?.handleLoginWepinError(WepinError.INVALID_LOGIN_SESSION)
            return _wepinLoginManager?.getWepinUserFuture()
                ?: CompletableFuture<WepinUser>().apply {
                    completeExceptionally(WepinError.NOT_INITIALIZED_NETWORK)
                }
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
        if (_wepinLoginManager == null) {
            throw WepinError.NOT_INITIALIZED_ERROR
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

    fun finalize(): Boolean {
        if (!_isInitialized) {
            throw WepinError.NOT_INITIALIZED_ERROR
        }
        _wepinSessionManager = null
        _wepinNetwork = null
        _wepinLoginManager?.clear()
        _wepinLoginManager = null
        _isInitialized = false
        return true
    }
}