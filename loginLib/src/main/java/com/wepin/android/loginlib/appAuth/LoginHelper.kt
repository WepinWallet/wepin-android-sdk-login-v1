package com.wepin.android.loginlib.appAuth

import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.commonlib.types.WepinUser
import com.wepin.android.core.network.WepinFirebase
import com.wepin.android.core.network.WepinNetwork
import com.wepin.android.core.storage.WepinStorageManager
import com.wepin.android.core.types.firebase.EmailAndPasswordRequest
import com.wepin.android.core.types.firebase.GetRefreshIdTokenRequest
import com.wepin.android.core.types.firebase.ResetPasswordRequest
import com.wepin.android.core.types.firebase.VerifyEmailRequest
import com.wepin.android.core.types.storage.StorageDataType
import com.wepin.android.core.types.wepin.OAuthTokenRequest
import com.wepin.android.core.types.wepin.OAuthTokenResponse
import com.wepin.android.core.types.wepin.PasswordStateRequest
import com.wepin.android.core.types.wepin.VerifyRequest
import com.wepin.android.core.types.wepin.VerifyResponse
import com.wepin.android.core.utils.Log
import com.wepin.android.loginlib.manager.CompletableFutureManager
import com.wepin.android.loginlib.types.FBToken
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.LoginWithEmailParams
import com.wepin.android.loginlib.types.OauthTokenParam
import com.wepin.android.loginlib.types.OauthTokenType
import com.wepin.android.loginlib.types.Providers
import com.wepin.android.loginlib.utils.hashPassword
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

internal class LoginHelper(
    private val networkManager: WepinNetwork,
    private val firebaseNetwork: WepinFirebase,
    private val completableFutureManager: CompletableFutureManager
) {
    internal lateinit var appAuthRedirectUrl: String
        private set

    fun init(redirectUrl: String) {
        appAuthRedirectUrl = redirectUrl
    }

    fun handleOAuthResult(provider: String, token: String, tokenType: OauthTokenType) {
        completableFutureManager.completeOAuthSuccess(
            LoginOauthResult(provider, token, tokenType)
        )
    }

    fun handleOAuthError(error: Exception) {
        completableFutureManager.completeOAuthError(error)
    }

    fun handleLoginResult(loginResult: LoginResult) {
        completableFutureManager.completeLoginSuccess(loginResult)
    }

    fun handleLoginError(error: Exception) {
        completableFutureManager.completeLoginError(error)
    }

    fun handleLoginError(error: Throwable) {
        completableFutureManager.completeLoginError(error)
    }

    fun handleLoginWepinResult(wepinUser: WepinUser) {
        completableFutureManager.completeWepinUserSuccess(wepinUser)
    }

    fun handleLoginWepinError(error: Exception) {
        completableFutureManager.completeWepinUserError(error)
    }

    fun handleLoginWepinError(error: Throwable) {
        completableFutureManager.completeWepinUserError(error)
    }

    fun getOauthTokenWithWepin(param: OauthTokenParam, tokenType: OauthTokenType) {
        val body = OAuthTokenRequest(
            code = param.code,
            clientId = param.clientId,
            state = param.state,
            redirectUri = appAuthRedirectUrl,
            codeVerifier = param.codeVerifier
        )

        networkManager.oauthTokenRequest(param.provider, body)
            .whenComplete { response, error ->
                handleTokenResponse(param.provider, tokenType, response, error)
            }
    }

    fun verifySignUpFirebase(params: LoginWithEmailParams) {
        val localeId = if (params.locale == "ko") 1 else 2
        networkManager.verify(
            VerifyRequest(
                type = "create",
                email = params.email,
                localeId = localeId
            )
        ).whenComplete { verifyResponse, verifyError ->
            if (verifyError != null) {
                if (verifyError.message?.contains("400") == true) {
                    handleLoginError(WepinError.INVALID_EMAIL_DOMAIN)
                } else {
                    handleLoginError(WepinError.FAILED_SEND_EMAIL)
                }
            }
            if (verifyResponse.result) {
                if (verifyResponse.oobVerify != null && verifyResponse.oobReset != null) {
                    signUpFirebase(params, verifyResponse)
                } else {
                    handleLoginError(WepinError.REQUIRED_EMAIL_VERIFIED)
                }
            }
        }
    }

    private fun signUpFirebase(params: LoginWithEmailParams, verifyResponse: VerifyResponse) {
        firebaseNetwork.resetPassword(
            ResetPasswordRequest(
                oobCode = verifyResponse.oobReset!!,
                newPassword = params.password
            )
        )
            .whenComplete { resetPasswordResponse, resetPasswordError ->
                if (resetPasswordError != null || resetPasswordResponse.email.trim()
                        .lowercase() != params.email.trim()
                ) {
                    handleLoginError(WepinError.FAILED_PASSWORD_SETTING)
                } else {
                    firebaseNetwork.verifyEmail(VerifyEmailRequest(oobCode = verifyResponse.oobVerify!!))
                        .whenComplete { verifyEmailRes, verifyEmailError ->
                            if (verifyEmailError != null || verifyEmailRes.email.trim()
                                    .lowercase() != params.email.trim()
                            ) {
                                handleLoginError(WepinError.FAILED_EMAIL_VERIFICATION)
                            } else {
                                loginWithEmailAndResetPasswordState(params.email, params.password)
                            }
                        }

                }
            }
    }

    fun loginWithEmailAndResetPasswordState(email: String, password: String) {
        WepinStorageManager.deleteAllStorage()
        networkManager.getUserPasswordState(email.trim())
            .whenComplete { getPasswordStateRes, getPasswordStateError ->
                if (getPasswordStateError != null) {
                    if (!isFirstEmailUser(getPasswordStateError.message.toString())) {
                        handleLoginError(getPasswordStateError)
                    }
                }
                val encryptedPassword = hashPassword(password)
                val isChangeRequired =
                    getPasswordStateRes?.isPasswordResetRequired == true || getPasswordStateError != null
                val firstPw = if (isChangeRequired) password else encryptedPassword
                firebaseNetwork.signInWithEmailPassword(
                    EmailAndPasswordRequest(
                        email.trim(),
                        firstPw
                    )
                ).whenComplete { signInResponse, signInError ->
                    if (signInError != null) handleLoginError(signInError)
                    val idToken: String = signInResponse.idToken
                    val refreshToken: String = signInResponse.refreshToken

                    if (isChangeRequired) {
                        changePassword(
                            encryptedPassword,
                            FBToken(idToken, refreshToken)
                        ).thenApply { token ->
                            if (token != null) {
                                WepinStorageManager.setStorage<StorageDataType>(
                                    "firebase:wepin",
                                    StorageDataType.FirebaseWepin(
                                        idToken = token.idToken,
                                        refreshToken = token.refreshToken,
                                        provider = "email"
                                    )
                                )
                                handleLoginResult(
                                    LoginResult(
                                        provider = Providers.EMAIL,
                                        token = token
                                    )
                                )
                            }
                        }?.exceptionally {
                            handleLoginError(it)
                        } ?: handleLoginError(WepinError.generalUnKnownEx("failed password set"))
                    } else {
                        handleLoginResult(
                            LoginResult(
                                provider = Providers.EMAIL,
                                token = FBToken(idToken, refreshToken)
                            )
                        )
                    }
                }
            }
    }

    fun doFirebaseLoginWithCustomToken(token: String, type: String) {
        firebaseNetwork.signInWithCustomToken(token).thenApply { res ->
            val loginResult =
                LoginResult(Providers.fromValue(type)!!, FBToken(res.idToken, res.refreshToken))
            WepinStorageManager.setStorage<StorageDataType>(
                "firebase:wepin",
                StorageDataType.FirebaseWepin(
                    idToken = res.idToken,
                    refreshToken = res.refreshToken,
                    provider = type
                )
            )
            handleLoginResult(loginResult)
        }
    }

    fun getRefreshIdToken(provider: Providers, refreshToken: String) {
        firebaseNetwork.getRefreshIdToken(GetRefreshIdTokenRequest(refreshToken))
            .whenComplete { response, error ->
                if (error != null) {
                    handleLoginError(WepinError.generalUnKnownEx("${error.message}"))
                } else {
                    val token = FBToken(
                        idToken = response.id_token,
                        refreshToken = response.refresh_token
                    )
                    val loginResult = LoginResult(provider = provider, token = token)
                    WepinStorageManager.setStorage(
                        "firebase:wepin",
                        StorageDataType.FirebaseWepin(
                            idToken = token.idToken,
                            refreshToken = token.refreshToken,
                            provider = provider.value
                        )
                    )
                    handleLoginResult(loginResult)
                }
            }
    }

    private fun changePassword(password: String, token: FBToken): CompletableFuture<FBToken?> {
        return networkManager.login(token.idToken).thenCompose { loginResponse ->
            firebaseNetwork.updatePassword(token.idToken, password).thenCompose { updatePWRes ->
                val passwordStateRequest = PasswordStateRequest(false)
                networkManager.updateUserPasswordState(
                    loginResponse.userInfo.userId,
                    passwordStateRequest
                ).thenApply {
                    FBToken(updatePWRes.idToken, updatePWRes.refreshToken)
                }
            }
        }
    }

    private fun isFirstEmailUser(errorString: String): Boolean {
        val jsonString = errorString.substringAfter("Error Message: ")

        try {
            // JSON 파싱
            val jsonObject = JSONObject(jsonString)

            // 필요한 필드 값 추출
            val status = jsonObject.getInt("status")
            val message = jsonObject.getString("message")

            // 조건 검사
            val isStatus400 = (status == 400)
            val isMessageContainsNotExist = message.contains("not exist")

            // 결과 출력
            return isStatus400 && isMessageContainsNotExist
        } catch (e: Exception) {
            return false
        }
    }

    private fun handleTokenResponse(
        provider: String,
        authType: OauthTokenType,
        response: OAuthTokenResponse?,
        error: Throwable?
    ) {
        when {
            error != null -> handleOAuthError(WepinError.FAILED_LOGIN)
            response != null -> processTokenResponse(provider, authType, response)
            else -> handleOAuthError(WepinError.INVALID_TOKEN)
        }
    }

    private fun processTokenResponse(
        provider: String,
        authType: OauthTokenType,
        response: OAuthTokenResponse
    ) {
        when (authType) {
            OauthTokenType.ACCESS_TOKEN -> handleOAuthResult(
                provider,
                response.access_token,
                OauthTokenType.ACCESS_TOKEN
            )

            else -> response.id_token?.let {
                handleOAuthResult(provider, it, OauthTokenType.ID_TOKEN)
            } ?: handleOAuthError(WepinError.INVALID_TOKEN)
        }
    }
}