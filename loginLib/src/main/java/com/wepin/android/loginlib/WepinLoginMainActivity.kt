package com.wepin.android.loginlib

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.wepin.android.commonlib.error.WepinError
import com.wepin.android.loginLib.R
import com.wepin.android.loginlib.manager.WepinLoginManager
import com.wepin.android.loginlib.types.OauthTokenParam
import com.wepin.android.loginlib.types.OauthTokenType
import com.wepin.android.networklib.types.wepin.OAuthProviderInfo
import com.wepin.android.storage.utils.Log
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
    private val TAG = this.javaClass.name
    private var authService: AuthorizationService? = null
    private var authState: AuthState? = null

    private val wepinLoginManager = WepinLoginManager.getInstance()
    private var redirectUri: Uri? = null
    private var provider: String? = null
    private var clientId: String? = null
    private var authUri: Uri? = null
    private var tokenUri: Uri? = null
    private var tokenType: OauthTokenType? = null
    private var token: String? = null

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val resp = AuthorizationResponse.fromIntent(data!!)
        val ex = AuthorizationException.fromIntent(data)
        handleAuthResult(resp, ex)
        authService?.customTabManager?.dispose()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d(TAG, "onCreate")
            super.onCreate(savedInstanceState)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            setContentView(R.layout.activity_wepin_login_main)
            val bundle = intent.extras

            val providerInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle?.getParcelable("providerInfo", OAuthProviderInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle?.getParcelable("providerInfo")
            }

            if (providerInfo == null) {
                wepinLoginManager.loginHelper?.handleOAuthError(
                    WepinError("providerInfo is not exist")
                )
                finish()
                return
            }
            provider = providerInfo.provider
            clientId = bundle?.getString("clientId").toString()
            redirectUri = Uri.parse(wepinLoginManager.redirectUrl)
            authUri = Uri.parse(providerInfo.authorizationEndpoint)
            tokenUri = Uri.parse(providerInfo.tokenEndpoint)
            tokenType =
                if (providerInfo.supportIdToken()) OauthTokenType.ID_TOKEN else OauthTokenType.ACCESS_TOKEN
            processLoginOauth2(clientId!!)
        } catch (e: Exception) {
            wepinLoginManager.loginHelper?.handleOAuthError(e)
            e.printStackTrace()
        }
    }

    private fun processLoginOauth2(clientId: String) {
        Log.d(TAG, "processLoginOauth2")
        if (authUri == null || tokenUri == null) {
            wepinLoginManager.loginHelper?.handleOAuthError(WepinError("invalid endpoint"))
            return
        }

        val serviceConfig = AuthorizationServiceConfiguration(
            authUri!!,
            tokenUri!!
        )
        authState = AuthState(serviceConfig)
        loginAppauth(serviceConfig, clientId, provider!!)
    }

    private fun loginAppauth(
        serviceConfig: AuthorizationServiceConfiguration,
        clientId: String,
        provider: String
    ) {
        Log.d(TAG, "loginAppauth")
        if (redirectUri == null) {
            wepinLoginManager.loginHelper?.handleOAuthError(WepinError("invalid redirectUrl"))
            return
        }

        val builder = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri!!
        )

        if (provider == "apple") builder.setResponseMode("form_post")

        builder.setPrompt("select_account")

        val authRequest = builder.build()
        authService = AuthorizationService(this)

        val authIntent = authService!!.getAuthorizationRequestIntent(authRequest)
        authLauncher.launch(authIntent)
    }

    private fun handleAuthResult(resp: AuthorizationResponse?, ex: AuthorizationException?) {
        authState = AuthState(resp, ex)

        when {
            ex != null -> {
                if (ex == USER_CANCELED_AUTH_FLOW) {
                    wepinLoginManager.loginHelper?.handleOAuthError(
                        WepinError(
                            WepinError.Companion.ErrorCode.FAILED_LOGIN.ordinal,
                            "user_canceled"
                        )
                    )
                } else {
                    wepinLoginManager.loginHelper?.handleOAuthError(ex)
                }
            }

            resp == null -> {
                wepinLoginManager.loginHelper?.handleOAuthError(WepinError.FAILED_LOGIN)
            }

            else -> {
                getOauthToken(resp)
            }
        }
    }

    private fun getOauthToken(resp: AuthorizationResponse) {
        val tokenExchangeRequest = resp.createTokenExchangeRequest()

        if (provider == "discord") {
            getOauthTokenWithAppAuth(tokenExchangeRequest)
        } else {
            val param = OauthTokenParam(
                provider = provider!!,
                clientId = clientId!!,
                codeVerifier = tokenExchangeRequest.codeVerifier,
                code = resp.authorizationCode!!,
                state = resp.state,
            )
            wepinLoginManager.loginHelper?.getOauthTokenWithWepin(param, tokenType!!)
        }
    }

    private fun getOauthTokenWithAppAuth(tokenExchangeRequest: TokenRequest) {
        Log.d(TAG, "getOauthTokenWithAppAuth")
        authService?.performTokenRequest(tokenExchangeRequest) { response, exception ->
            if (exception != null) {
                wepinLoginManager.loginHelper?.handleOAuthError(exception)
            } else if (response != null) {
                token =
                    if (tokenType == OauthTokenType.ID_TOKEN) response.idToken else response.accessToken

                token?.let {
                    val tokenType =
                        if (tokenType == OauthTokenType.ID_TOKEN) OauthTokenType.ID_TOKEN else OauthTokenType.ACCESS_TOKEN
                    wepinLoginManager.loginHelper?.handleOAuthResult(provider!!, it, tokenType)
                }
                authState!!.update(response, null)
            }
        }
    }
}