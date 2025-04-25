package com.wepin.sample

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wepin.android.commonlib.types.LoginOauthAccessTokenRequest
import com.wepin.android.commonlib.types.LoginOauthIdTokenRequest
import com.wepin.android.loginlib.WepinLogin
import com.wepin.android.loginlib.types.LoginOauth2Params
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.LoginWithEmailParams
import com.wepin.android.loginlib.types.OauthTokenType
import com.wepin.android.loginlib.types.WepinLoginOptions
import com.wepin.sample.ui.theme.WepinAndroidSDKTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WepinAndroidSDKTheme {
                WepinLoginTestScreen()
            }
        }
    }
}

class WepinLoginViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "WepinLoginModel"
    private val context: Context
        get() = getApplication<Application>().applicationContext

    private var _status = mutableStateOf("Not Initialized")
    val status: State<String> = _status
    private var _lastAction = mutableStateOf("No action yet")
    val lastAction: State<String> = _lastAction
    private var loginToken: MutableState<LoginOauthResult?> = mutableStateOf(null)
    private var loginResult: MutableState<LoginResult?> = mutableStateOf(null)

    private var wepinLogin: WepinLogin? = null

    fun initializeWepinLogin(context: Context, appId: String, appKey: String) {
        wepinLogin = WepinLogin(
            WepinLoginOptions(
                context = context,
                appId = appId,
                appKey = appKey,
            )
        )
    }

    fun initialize() {
        wepinLogin?.init()?.whenComplete { result, error ->
            if (error != null) {
                updateStatus("initialize", "$error")
            } else {
                updateStatus("initialize", "$result")
            }
        }
    }

    fun checkInitializationStatus() {
        val result =
            if (wepinLogin?.isInitialized() == true) "Widget is Initialized" else "Not Initialized"
        updateStatus("checkInitializationStatus", result)
    }

    fun signUpWithEmail() {
        Log.d(TAG, "signUpWithEmail")
        val params = LoginWithEmailParams(
            email = "email",
            password = "password",
            locale = "ko"
        )
        wepinLogin?.signUpWithEmailAndPassword(params)?.whenComplete { result, error ->
            Log.d(TAG, "signUpWithEmail End")
            if (error != null) {
                updateStatus("signUpWithEmail", "$error")
            } else {
                updateStatus("signUpWithEmail", "$result")
            }
        }
    }

    fun loginWithEmail() {
        val params = LoginWithEmailParams(
            email = "email",
            password = "password"
        )
        wepinLogin?.loginWithEmailAndPassword(params)?.whenComplete { result, error ->
            if (error != null) {
                updateStatus("loginWithEmail", "$error")
            } else {
                loginResult.value = result
                updateStatus("loginWithEmail", "$result")
            }
        }
    }

    fun loginWithOauth(provider: String) {
        try {
            val clientId = when (provider) {
                "google" -> context.getString(R.string.default_google_web_client_id)
                "apple" -> context.getString(R.string.default_apple_client_id)
                "naver" -> context.getString(R.string.default_naver_client_id)
                "discord" -> context.getString(R.string.default_discord_client_id)
                "facebook" -> context.getString(R.string.default_facebook_client_id)
                "line" -> context.getString(R.string.default_line_client_id)
                else -> "invalid_client_Id"
            }
            val params = LoginOauth2Params(
                provider,
                clientId
            )
            wepinLogin?.loginWithOauthProvider(params)?.whenComplete { result, error ->
                if (error != null) {
                    updateStatus("loginWithOauth(provider: $provider)", "$error")
                } else {
//                    loginWithToken(result)
                    loginToken.value = result
                    updateStatus("loginWithOauth(provider: $provider)", "$result")
                }
            }
        } catch (error: Exception) {
            updateStatus("loginWithOauthProvider Fail with error", "$error")
        }
    }

    fun loginWithToken() {
        if (loginToken.value == null) {
            _status.value = "Error: LoginToken is null"
            updateStatus("Error: LoginToken is null", "")
            return
        }
        when (loginToken.value!!.type) {
            OauthTokenType.ID_TOKEN -> {
                wepinLogin?.loginWithIdToken(LoginOauthIdTokenRequest(idToken = loginToken.value!!.token))
                    ?.whenComplete { result, error ->
                        if (error != null) {
                            updateStatus("loginWithIdToken", "$error")
                        } else {
                            loginResult.value = result
                            updateStatus("loginWithIdToken", "$result")
                        }
                    }
            }

            OauthTokenType.ACCESS_TOKEN -> {
                try {
                    wepinLogin?.loginWithAccessToken(
                        LoginOauthAccessTokenRequest(
                            provider = loginToken.value!!.provider,
                            accessToken = loginToken.value!!.token,
                        )
                    )?.whenComplete { result, error ->
                        if (error != null) {
                            updateStatus("loginWithAccessToken", "$error")
                        } else {
                            loginResult.value = result
                            updateStatus("loginWithAccessToken", "$result")
                        }
                    }
                } catch (error: Exception) {
                    Log.d("loginWithToken", "error:$error")
                }
            }
        }

    }

    fun loginWithIdToken() {
        try {
            wepinLogin?.loginWithIdToken(
                LoginOauthIdTokenRequest(
                    idToken = loginToken.value?.token ?: ""
                )
            )
                ?.whenComplete { result, error ->
                    if (error != null) {
                        updateStatus("loginWithIdToken", "$error")
                    } else {
                        loginResult.value = result
                        updateStatus("loginWithIdToken", "$result")
                    }
                }
        } catch (error: Exception) {
            updateStatus("loginWithIdToken", "$error")
        }
    }

    fun loginWithAccessToken() {
        try {
            wepinLogin?.loginWithAccessToken(
                LoginOauthAccessTokenRequest(
                    provider = loginToken.value?.provider ?: "",
                    accessToken = loginToken.value?.token ?: "",
                )
            )?.whenComplete { result, error ->
                if (error != null) {
                    updateStatus("loginWithAccessToken", "$error")
                } else {
                    loginResult.value = result
                    updateStatus("loginWithAccessToken", "$result")
                }
            }
        } catch (error: Exception) {
            Log.d("loginWithToken", "error:$error")
        }
    }

    fun loginWepin() {
        if (loginResult.value == null) {
            updateStatus("loginWepin", "loginResult is needed")
        } else {
            wepinLogin?.loginWepin(loginResult.value!!)?.whenComplete { result, error ->
                if (error != null) {
                    updateStatus("loginWepin", "$error")
                } else {
                    updateStatus("loginWepin", "$result")
                }
            }
        }
    }

    fun getWepinUser() {
        wepinLogin?.getCurrentWepinUser()?.whenComplete { result, error ->
            if (error != null) {
                updateStatus("getWepinUser", "$error")
            } else {
                updateStatus("getWepinUser", "$result")
            }
        }
    }

    fun logoutWepin() {
        wepinLogin?.logoutWepin()?.whenComplete { result, error ->
            if (error != null) {
                updateStatus("logoutWepin", "$error")
            } else {
                updateStatus("logoutWepin", "$result")
            }
        }
    }

    fun getRefreshToken(refresh: Boolean) {
        val prevToken = if (refresh) loginResult.value else null
        wepinLogin?.getRefreshFirebaseToken(prevToken)?.whenComplete { result, error ->
            if (error != null) {
                updateStatus("getRefreshToken($refresh)", "$error")
            } else {
                updateStatus("getRefreshToken($refresh)", "$result")
            }
        }
    }

    fun finalizeLogin() {
        wepinLogin?.finalize()
        updateStatus("finalize", "")
    }

    private fun updateStatus(action: String, message: String) {
        _lastAction.value = action
        _status.value = message
    }
}

@Composable
fun WepinLoginTestScreen(
    viewModel: WepinLoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val appId = remember { context.getString(R.string.wepin_app_id) }
    val appKey = remember { context.getString(R.string.wepin_app_key) }

    LaunchedEffect(Unit) {
        viewModel.initializeWepinLogin(context, appId, appKey)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(16.dp)
    ) {
        HeaderSection()
        ButtonSection(
            modifier = Modifier.weight(2f),
            onInitialize = { viewModel.initialize() },
            onCheckStatus = { viewModel.checkInitializationStatus() },
            onSignUp = { viewModel.signUpWithEmail() },
            onLogin = { type, provider ->
                when (type) {
                    "basic" -> viewModel.loginWithEmail()
                    "oauth" -> viewModel.loginWithOauth(provider)
                    "idToken" -> viewModel.loginWithIdToken()
                    "accessToken" -> viewModel.loginWithAccessToken()
                    "token" -> viewModel.loginWithToken()
                    "wepin" -> viewModel.loginWepin()
                }
            },
            onRefreshToken = { refresh ->
                viewModel.getRefreshToken(refresh)
            },
            onLogout = { viewModel.logoutWepin() },
            onGetUser = { viewModel.getWepinUser() },
            onFinalize = { viewModel.finalizeLogin() }
        )
        ResultSection(
            modifier = Modifier.weight(1f),
            action = viewModel.lastAction.value,
            status = viewModel.status.value
        )
    }
}

@Composable
private fun HeaderSection() {
    Text("Wepin Login Test", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
private fun ButtonSection(
    modifier: Modifier = Modifier,
    onInitialize: () -> Unit,
    onCheckStatus: () -> Unit,
    onSignUp: () -> Unit,
    onLogin: (type: String, provider: String) -> Unit,
    onRefreshToken: (refresh: Boolean) -> Unit,
    onLogout: () -> Unit,
    onGetUser: () -> Unit,
    onFinalize: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WepinButton(text = "Initialize", onClick = onInitialize)
        WepinButton(text = "Check Initialization Status", onClick = onCheckStatus)
        WepinButton(text = "Email Signup", onClick = onSignUp)
        WepinButton(text = "Email Login", onClick = { onLogin("basic", "email") })
        WepinButton(text = "OAuth Login (Google)", onClick = { onLogin("oauth", "google") })
        WepinButton(text = "OAuth Login (Apple)", onClick = { onLogin("oauth", "apple") })
        WepinButton(text = "OAuth Login (Discord)", onClick = { onLogin("oauth", "discord") })
        WepinButton(text = "OAuth Login (Naver)", onClick = { onLogin("oauth", "naver") })
        WepinButton(text = "OAuth Login (Facebook)", onClick = { onLogin("oauth", "facebook") })
        WepinButton(text = "OAuth Login (Line)", onClick = { onLogin("oauth", "line") })
        WepinButton(text = "OAuth Login (Kakao)", onClick = { onLogin("oauth", "kakao") })
        WepinButton(text = "IdToken Login", onClick = { onLogin("idToken", "") })
        WepinButton(text = "AccessToken Login", onClick = { onLogin("accessToken", "") })
        WepinButton(text = "RefreshFirebaseToken", onClick = { onRefreshToken(false) })
        WepinButton(text = "RefreshFirebaseToken (refresh)", onClick = { onRefreshToken(true) })
        WepinButton(text = "Wepin Login", onClick = { onLogin("wepin", "") })
        WepinButton(text = "Wepin Logout", onClick = onLogout)
        WepinButton(text = "Get Wepin User", onClick = onGetUser)
        WepinButton(text = "WepinLogin finalize", onClick = onFinalize)
    }
}

@Composable
private fun WepinButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun ResultSection(
    modifier: Modifier = Modifier,
    action: String,
    status: String
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .background(Color.White)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Result", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = action, style = MaterialTheme.typography.bodyLarge)
                Text(text = status, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}