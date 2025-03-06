package com.wepin.android.loginlib.appAuth

import com.wepin.android.loginlib.error.WepinError
import com.wepin.android.loginlib.manager.WepinLoginManager
import com.wepin.android.loginlib.storage.WepinStorageManager
import com.wepin.android.loginlib.types.ErrorCode
import com.wepin.android.loginlib.types.FBToken
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.LoginWithEmailParams
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.OauthTokenParam
import com.wepin.android.loginlib.types.OauthTokenType
import com.wepin.android.loginlib.types.Providers
import com.wepin.android.loginlib.types.WepinLoginError
import com.wepin.android.loginlib.types.network.OAuthTokenRequest
import com.wepin.android.loginlib.types.network.PasswordStateRequest
import com.wepin.android.loginlib.types.network.VerifyRequest
import com.wepin.android.loginlib.types.network.VerifyResponse
import com.wepin.android.loginlib.types.network.firebase.EmailAndPasswordRequest
import com.wepin.android.loginlib.types.network.firebase.ResetPasswordRequest
import com.wepin.android.loginlib.types.network.firebase.VerifyEmailRequest
import com.wepin.android.loginlib.utils.hashPassword
import org.json.JSONObject
import java.util.concurrent.CompletableFuture

internal class LoginHelper(
    private val wepinLoginManager: WepinLoginManager,
) {
    fun onWepinOauthLoginResult(provider: String, token: String) {
        val providerValue = Providers.fromValue(provider)
        if(providerValue == null) {
            wepinLoginManager.loginOauthCompletableFuture.completeExceptionally(WepinError.INVALID_LOGIN_PROVIDER)
        } else {
            setLoginOauthResult(token, provider)

        }
    }

    fun onWepinOauthLoginError(code: ErrorCode?, errorMessage:String? = null) {
        if(code != null){
            wepinLoginManager.loginOauthCompletableFuture.completeExceptionally(WepinError(WepinLoginError.getError(code)))
            return
        }
        wepinLoginManager.loginOauthCompletableFuture.completeExceptionally(WepinError.generalUnKnownEx(errorMessage))
    }

    private fun setLoginOauthResult(token:String, provider: String) : CompletableFuture<LoginOauthResult> {
        when(provider){
            "google", "apple" -> {
                wepinLoginManager.loginOauthCompletableFuture.complete(LoginOauthResult(provider, token, OauthTokenType.ID_TOKEN ))
            }
            "discord", "naver" -> {
                wepinLoginManager.loginOauthCompletableFuture.complete(LoginOauthResult(provider, token, OauthTokenType.ACCESS_TOKEN ))
            }
            else -> {
                wepinLoginManager.loginOauthCompletableFuture.completeExceptionally(WepinError.INVALID_LOGIN_PROVIDER)
            }
        }
        return wepinLoginManager.loginOauthCompletableFuture
    }

    fun doFirebaseLoginWithCustomToken(token:String, type: Providers) :CompletableFuture<LoginResult>? {
        return wepinLoginManager.wepinFirebaseManager?.signInWithCustomToken(token)?.thenApply { res ->
            val loginResult = LoginResult(type, FBToken(res.idToken, res.refreshToken))
            WepinStorageManager.setFirebaseUser(loginResult)
            loginResult
        }
    }

    private fun signUpFirebase(params: LoginWithEmailParams, verifyResponse: VerifyResponse) : CompletableFuture<LoginResult> {
        wepinLoginManager.wepinFirebaseManager?.resetPassword(ResetPasswordRequest(oobCode = verifyResponse.oobReset!!, newPassword = params.password))
            ?.whenComplete { resetPasswordResponse, resetPasswordError ->
                if(resetPasswordError != null || resetPasswordResponse.email.trim().lowercase() != params.email.trim()){
                    wepinLoginManager.loginCompletableFuture.completeExceptionally(WepinError.FAILED_PASSWORD_SETTING)
                } else {
                    wepinLoginManager.wepinFirebaseManager!!.verifyEmail(
                        VerifyEmailRequest(oobCode = verifyResponse.oobVerify!!)
                    ).whenComplete { verifyEmailRes, verifyEmailError ->
                        if(verifyEmailError != null || verifyEmailRes.email.trim().lowercase() !== params.email.trim()){
                            wepinLoginManager.loginCompletableFuture.completeExceptionally(WepinError.FAILED_EMAIL_VERIFIED)
                        }else {
                            loginWithEmailAndResetPasswordState(params.email, params.password)
                        }
                    }
                }
            }
        return wepinLoginManager.loginCompletableFuture
    }

    fun verifySignUpFirebase(params: LoginWithEmailParams): CompletableFuture<LoginResult> {
        val localeId = if(params.locale == "ko") 1 else 2
        wepinLoginManager.wepinNewtorkManager?.verify(VerifyRequest(type = "create", email=params.email, localeId = localeId))
            ?.whenComplete { verifyResponse, verifyError ->
                if(verifyError != null) {
                    if(verifyError.message?.contains("400") == true){
                        wepinLoginManager.loginCompletableFuture.completeExceptionally(WepinError.INVALID_EMAIL_DOMAIN)
                    }else {
                        wepinLoginManager.loginCompletableFuture.completeExceptionally(WepinError.FAILED_SEND_EMAIL)
                    }
                    return@whenComplete
                }
                if(verifyResponse.result){
                    if (verifyResponse.oobVerify != null && verifyResponse.oobReset != null){
                        signUpFirebase(params, verifyResponse)
                    }else {
                        wepinLoginManager.loginCompletableFuture.completeExceptionally(WepinError.REQUIRED_EMAIL_VERIFIED)
                    }
                }
            }
        return wepinLoginManager.loginCompletableFuture
    }

    private fun changePassword(password: String, token: FBToken): CompletableFuture<FBToken?>? {
        return  wepinLoginManager.wepinNewtorkManager?.login(token.idToken)?.thenCompose { loginResponse ->
            wepinLoginManager.wepinFirebaseManager?.updatePassword(token.idToken, password)
                ?.thenCompose { updatePwResponse ->
                    val passwordStateRequest = PasswordStateRequest(false)
                    wepinLoginManager.wepinNewtorkManager!!.updateUserPasswordState(loginResponse.userInfo.userId, passwordStateRequest)
                        .thenApply {
                            FBToken(updatePwResponse.idToken, updatePwResponse.refreshToken)
                        }
                }
        }
    }

    private fun isFirstEmailUser(errorString :String):Boolean {
        // JSON 문자열 추출
        val jsonString = errorString.substringAfter("java.lang.Exception: ")

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
        }catch (e: Exception){
            return false
        }
    }

    fun loginWithEmailAndResetPasswordState(email: String, password: String): CompletableFuture<LoginResult> {
        WepinStorageManager.deleteAllStorage()
        wepinLoginManager.wepinNewtorkManager?.getUserPasswordState(email.trim())?.whenComplete { t, u ->
            if(u != null) {
                if(!isFirstEmailUser(u.message.toString())){
                    wepinLoginManager.loginCompletableFuture.completeExceptionally(u)
                }
            }
            val encryptedPassword = hashPassword(password)
            val isChangeRequired = t?.isPasswordResetRequired == true || u != null
            val firstPw = if(isChangeRequired) password else encryptedPassword
            wepinLoginManager.wepinFirebaseManager?.signInWithEmailPassword(EmailAndPasswordRequest(
                email.trim(),
                firstPw
            ))
                ?.whenComplete { signInResponse , e ->
                    if(e != null) wepinLoginManager.loginCompletableFuture.completeExceptionally(e)
                    val idToken: String = signInResponse.idToken
                    val refreshToken: String = signInResponse.refreshToken

                    if(isChangeRequired) {
                        changePassword(encryptedPassword, FBToken(idToken, refreshToken))
                            ?.thenApply { token ->
                                if (token != null) {
                                    val loginResult = LoginResult(
                                        Providers.EMAIL,
                                        token
                                    )
                                    WepinStorageManager.setFirebaseUser(loginResult)
                                    wepinLoginManager.loginCompletableFuture.complete(loginResult)
                                }
                            }?.exceptionally {
                                wepinLoginManager.loginCompletableFuture.completeExceptionally(it)
                            } ?: wepinLoginManager.loginCompletableFuture.completeExceptionally(
                            WepinError(
                                WepinLoginError.getError(
                                    ErrorCode.UNKNOWN_ERROR,
                                    "failed password set"
                                )
                            )
                        )
                    }else {
                        wepinLoginManager.loginCompletableFuture.complete(
                            LoginResult(
                                Providers.EMAIL,
                                FBToken(idToken, refreshToken)
                            )
                        )
                    }
                }
        }
        return wepinLoginManager.loginCompletableFuture
    }

    fun getOauthTokenWithWepin(param: OauthTokenParam){
        val networkManger = wepinLoginManager.wepinNewtorkManager
        val body  = OAuthTokenRequest(
            code = param.code,
            clientId = param.clientId,
            state = param.state,
            redirectUri = wepinLoginManager.appAuthRedirectUrl,
            codeVerifier=param.codeVerifier,
        )
        networkManger?.oauthTokenRequest(param.provider, body)?.whenComplete { oauthTokenResponse, oauthTokenError ->
            if(oauthTokenError != null) {
                wepinLoginManager.loginHelper?.onWepinOauthLoginError(ErrorCode.FAILED_LOGIN, oauthTokenError.message)
            }
            else if(oauthTokenResponse != null){
                if(param.provider == "naver") wepinLoginManager.loginHelper?.onWepinOauthLoginResult(param.provider, oauthTokenResponse.access_token)
                else wepinLoginManager.loginHelper?.onWepinOauthLoginResult(param.provider, oauthTokenResponse.id_token!!)
            } else {
                wepinLoginManager.loginHelper?.onWepinOauthLoginError(ErrorCode.INVALID_TOKEN)
            }
        }
    }
}