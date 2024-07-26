package com.wepin.android.loginlib

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.wepin.android.loginlib.const.AppAuthConst
import com.wepin.android.loginlib.manager.WepinLoginManager
import com.wepin.android.loginlib.types.ErrorCode
import com.wepin.android.loginlib.types.OauthTokenParam
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest

internal class WepinLoginMainActivity : ComponentActivity() {
    private val RC_AUTH = 100
    private var authService: AuthorizationService? = null
    private var authState: AuthState? = null

    private val wepinLoginManager = WepinLoginManager.getInstance()
    private var redirectUri: Uri? = null
    private var provider: String? = null
    private var clientId: String? = null
    private var token: String? = null
//    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

//    private var jwt : JWT? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        try{
            super.onCreate(savedInstanceState)
            // 배경을 투명하게 설정
            window.setBackgroundDrawableResource(android.R.color.transparent)
            setContentView(R.layout.activity_wepin_login_main)
            val bundle = intent.extras
            provider = bundle?.getString("provider").toString()
            clientId = bundle?.getString("clientId").toString()
            redirectUri = Uri.parse(wepinLoginManager.appAuthRedirectUrl)
            processLoginOauth2(provider!!, clientId!!)
        } catch (e:Exception) {
            e.message?.let { wepinLoginManager.loginHelper?.onWepinOauthLoginError(null, it) }
            e.printStackTrace();
        }
    }

    private fun processLoginOauth2(provider: String, clientId: String){
        val authUri = AppAuthConst.getAuthorizationEndpoint(provider)
        val tokenUri = AppAuthConst.getTokenEndpoint(provider)
        val serviceConfig = AuthorizationServiceConfiguration(
            authUri,  // authorization endpoint
            tokenUri
        )
        authState = AuthState(serviceConfig)
        loginAppauth(serviceConfig, clientId, provider)
    }


    private fun loginAppauth(serviceConfig: AuthorizationServiceConfiguration, clientId: String, provider: String) {
        if(redirectUri === null)  {
            wepinLoginManager.loginHelper?.onWepinOauthLoginError(null, "invalid redirectUrl")
            return
        }
        val builder = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri!!
        )

        // apple의 경우, scope에 email을 추가하면 response mode를 POST 로 해줘야 함!!!
        if(provider == "apple") builder.setResponseMode("form_post")

        if(provider == "discord")
            builder.setScopes("identify", "email")
        else builder.setScopes("email")
        builder.setPrompt("select_account")

        val authRequest = builder.build()
        authService =  AuthorizationService(this)

        val authIntent = authService!!.getAuthorizationRequestIntent(authRequest)
        if (authIntent != null) {
            startActivityForResult(authIntent, RC_AUTH)
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_AUTH) {
            val resp = AuthorizationResponse.fromIntent(data!!)
            val ex = AuthorizationException.fromIntent(data)
            // ... process the response or exception ...
            authState = AuthState(resp, ex)

            if (ex != null) {
                val message = ex.message ?: ex.errorDescription ?: "unknown"
                if(ex == USER_CANCELED_AUTH_FLOW) wepinLoginManager.loginHelper?.onWepinOauthLoginError(ErrorCode.USER_CANCELLED)
                else wepinLoginManager.loginHelper?.onWepinOauthLoginError(null, message)
            }else if(resp == null){
                wepinLoginManager.loginHelper?.onWepinOauthLoginError(ErrorCode.FAILED_LOGIN)
            }else {
                getOauthToken(resp)
            }
        } else {
            // ...
        }
        authService?.customTabManager?.dispose()
        finish()
    }

    private fun getOauthToken(resp:AuthorizationResponse){
        val tokenExchangeRequest = resp.createTokenExchangeRequest()

        if(provider == "discord") {
            // PKCE를 지원하는 discord만 AppAuth를 통해 토큰 받아옴..
            getOauthTokenWithAppAuth(tokenExchangeRequest)
        }else {
            val param = OauthTokenParam(
                provider = provider!!,
                clientId = clientId!!,
                codeVerifier = tokenExchangeRequest.codeVerifier,
                code = resp.authorizationCode!!,
                state = resp.state,
            )
            wepinLoginManager.loginHelper?.getOauthTokenWithWepin(param)
        }
    }
    private fun getOauthTokenWithAppAuth(tokenExchangeRequest: TokenRequest) {
        authService?.performTokenRequest(tokenExchangeRequest) { response, exception ->
            if (exception != null) {
                val message = exception.message ?: "unknown"
                wepinLoginManager.loginHelper?.onWepinOauthLoginError(null, message)
            } else {
                if (response != null) {
                    when (provider) {
                        "google", "apple" -> token = response.idToken
                        "naver", "discord" -> token = response.accessToken
                    }
                    token?.let {
                        provider?.let { it1 ->
                            wepinLoginManager.loginHelper?.onWepinOauthLoginResult(
                                it1, it
                            )
                        }
                    }
                    authState!!.update(response, null)
                }
            }
        }

    }
}