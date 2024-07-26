package com.wepin.loginlibrary

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.wepin.android.loginlib.WepinLogin
import com.wepin.android.loginlib.types.LoginOauth2Params
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.LoginWithEmailParams
import com.wepin.android.loginlib.types.OauthTokenType
import com.wepin.android.loginlib.types.WepinLoginOptions
import com.wepin.android.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.android.loginlib.types.network.LoginOauthIdTokenRequest
import com.wepin.loginlibrary.ui.theme.LoginLibraryTheme
import java.util.concurrent.CompletableFuture

class MainActivity : ComponentActivity() {
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private lateinit var wepinLogin: WepinLogin

    private var itemListView: ListView? = null
    private var tvResult: TextView? = null
    private lateinit var testItem: Array<String>

    private var gso:GoogleSignInOptions? = null
    private var mGoogleSignInClient : GoogleSignInClient? = null

    private var loginResult: LoginResult? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_example_main)

        initView()

        //for Google login
        registerActivityResult()
        val wepinLoginOptions =  WepinLoginOptions(
            context = this,
            appId=resources.getString(R.string.wepin_app_id),
            appKey = resources.getString(R.string.wepin_app_key)
        )
        wepinLogin = WepinLogin(wepinLoginOptions)
    }

    override fun onNewIntent(intent: Intent?) {
        println("onNewIntent")
        super.onNewIntent(intent)

    }
    override fun onResume() {
        println("onResume")
        super.onResume()

    }

    private fun registerActivityResult() {
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_google_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = gso?.let { GoogleSignIn.getClient(this, it) }

        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            println("registerActivityResult - $result")
            if (result.resultCode == Activity.RESULT_OK) {
                // Intent의 결과 처리
//                val data: Intent? = result.data
                // 결과 사용
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                // account.getIdToken()으로 ID 토큰을 얻고 서버로 전송하여 사용자 인증을 진행할 수 있습니다.
                val token = account.idToken
                loginIdToken(resources.getString(R.string.item_idToken_google_login), token)
            }
        }
    }

    private fun initView() {
        try {
            itemListView = findViewById(R.id.lv_menu)
            tvResult = findViewById(R.id.tv_result)
            testItem = arrayOf(
                resources.getString(R.string.item_init),
                resources.getString(R.string.item_isInitialized),
                resources.getString(R.string.item_email_signup),
                resources.getString(R.string.item_email_login),
                resources.getString(R.string.item_oauth_google_login),
                resources.getString(R.string.item_oauth_apple_login),
                resources.getString(R.string.item_oauth_discord_login),
                resources.getString(R.string.item_oauth_naver_login),
                resources.getString(R.string.item_idToken_google_login),
                resources.getString(R.string.item_accessToken_discord_login),
                resources.getString(R.string.item_getRefreshFirbaseToken),
                resources.getString(R.string.item_wepin_login),
                resources.getString(R.string.item_wepin_logout),
                resources.getString(R.string.item_getWepinUser),
                resources.getString(R.string.item_sign_token),
                resources.getString(R.string.item_finalize),
            )
            val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
                this@MainActivity,
                android.R.layout.simple_list_item_1,
                testItem
            )
            itemListView?.adapter = adapter
            itemListView?.itemsCanFocus = true
            itemListView?.choiceMode = ListView.CHOICE_MODE_SINGLE
            itemListView?.onItemClickListener = OnItemClickListener { arg0, arg1, arg2, arg3 ->
                val operationItem = arg0.adapter.getItem(arg2).toString()
                if(operationItem != resources.getString(R.string.item_init)){
                    if(!wepinLoginIsInitialized(operationItem))
                    return@OnItemClickListener
                }
                when (operationItem) {
                    resources.getString(R.string.item_init) -> {
                        Log.d("MainAcivity", "clicked - $operationItem")
                        initWepinLogin(operationItem)
                    }
                    resources.getString(R.string.item_isInitialized) -> {
                        wepinLoginIsInitialized(operationItem, true)
                    }
                    resources.getString(R.string.item_email_signup) -> {
                        val loginOption = LoginWithEmailParams(
                            email = "email",
                            password = "password"
                        )
                        loginWepinProcess(operationItem, loginOption, wepinLogin::signUpWithEmailAndPassword)

                    }
                    resources.getString(R.string.item_email_login) -> {
                        val loginOption = LoginWithEmailParams(
                            email = "email",
                            password = "password"
                        )
                        loginWepinProcess(operationItem, loginOption, wepinLogin::loginWithEmailAndPassword)
                    }
                    resources.getString(R.string.item_oauth_google_login) -> {
                        val loginOption = LoginOauth2Params(
                            provider = "google",
                            clientId = getString(R.string.default_google_web_client_id),
                        )
                        loginWepinProcess(operationItem, loginOption, wepinLogin::loginWithOauthProvider, OauthTokenType.ID_TOKEN)
                    }
                    resources.getString(R.string.item_oauth_discord_login) -> {
                        val loginOption = LoginOauth2Params(
                            provider = "discord",
                            clientId = getString(R.string.default_discord_client_id),
                        )
                        loginWepinProcess(operationItem, loginOption, wepinLogin::loginWithOauthProvider, OauthTokenType.ACCESS_TOKEN)
                    }
                    resources.getString(R.string.item_oauth_naver_login) -> {
                        val loginOption = LoginOauth2Params(
                            provider = "naver",
                            clientId = getString(R.string.default_naver_client_id),
                        )
                        loginWepinProcess(operationItem, loginOption, wepinLogin::loginWithOauthProvider, OauthTokenType.ACCESS_TOKEN)
                    }
                    resources.getString(R.string.item_oauth_apple_login) -> {
                        val loginOption = LoginOauth2Params(
                            provider = "apple",
                            clientId = getString(R.string.default_apple_client_id),
                        )
                        loginWepinProcess(operationItem, loginOption, wepinLogin::loginWithOauthProvider, OauthTokenType.ID_TOKEN)
                    }
                    resources.getString(R.string.item_idToken_google_login) -> {
                        val signInIntent: Intent = mGoogleSignInClient!!.signInIntent
                        resultLauncher.launch(signInIntent)
                    }
                    resources.getString(R.string.item_accessToken_discord_login) -> {
                        val token = "AccessToken"
                        loginAccessToken(operationItem, "discord", token)
                    }
                    resources.getString(R.string.item_getRefreshFirbaseToken) -> {
                        wepinLogin.getRefreshFirebaseToken().whenComplete{ res, err ->
                            loginResult = res
                            if(err == null){
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    res
                                )
                            } else {
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    "fail"
                                )
                            }
                        }
                    }
                    resources.getString(R.string.item_wepin_login) -> {
                        if (loginResult == null){
                            tvResult?.text = String.format(
                                " Item : %s\n Result : %s",
                                operationItem,
                                "fail: invalid loginResult"
                            )
                            return@OnItemClickListener
                        }
                        wepinLogin.loginWepin(loginResult!!).whenComplete{ res, err ->
                            loginResult = null
                            if(err == null){
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    res
                                )
                            } else {
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    "fail"
                                )
                            }
                        }
                    }
                    resources.getString(R.string.item_wepin_logout) -> {
                        wepinLogin.logoutWepin().whenComplete { res, err ->
                            if (err == null) {
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    res
                                )
                            } else {
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    "fail"
                                )
                            }
                        }
                    }
                    resources.getString(R.string.item_getWepinUser) -> {
                        wepinLogin.getCurrentWepinUser().whenComplete{ res, err ->
                            if(err == null){
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    res
                                )
                            } else {
                                tvResult?.text = String.format(
                                    " Item : %s\n Result : %s",
                                    operationItem,
                                    "fail"
                                )
                            }
                        }
                    }
                    resources.getString(R.string.item_sign_token) -> {
                        val token = ""
                        val sign = wepinLogin.getSignForLogin(resources.getString(R.string.wepin_app_private_key), token)

                        tvResult?.text = String.format(
                            " Item : %s\n Result : %s",
                            operationItem,
                            sign
                        )
                    }
                    resources.getString(R.string.item_finalize) -> {
                        wepinLogin.finalize()

                        tvResult?.text = String.format(
                            " Item : %s\n Result : %s",
                            operationItem,
                            "success"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loginIdToken(operationItem: String, token:String?) {
        try {
            println("activity - token $token")
            if(token != null) {
                val sign = wepinLogin.getSignForLogin(resources.getString(R.string.wepin_app_private_key), token)
                val loginOption = LoginOauthIdTokenRequest(idToken =token, sign = sign)
                loginWepinProcess(operationItem, loginOption, wepinLogin::loginWithIdToken)
            } else {
                tvResult?.text = String.format(
                    " Item : %s\n Result : %s",
                    operationItem,
                    "fail"
                )
            }
        } catch (e: ApiException) {
            // 로그인 실패 처리
            println("activity - error $e")
            tvResult?.text = String.format(
                " Item : %s\n Result : %s",
                operationItem,
                "fail $e"
            )
        }
    }

    private fun loginAccessToken(operationItem: String, provider:String, token: String) {

        val sign = wepinLogin.getSignForLogin(resources.getString(R.string.wepin_app_private_key), token)
        Log.d(R.string.item_accessToken_discord_login.toString(), sign)
        val loginOption = LoginOauthAccessTokenRequest(
            provider = provider,
            accessToken = token, sign = sign
        )
        loginWepinProcess(operationItem, loginOption, wepinLogin::loginWithAccessToken)
    }

    private fun initWepinLogin(operationItem:String) {
        if (wepinLogin.isInitialized()) {
            tvResult?.text = String.format(
                " Item : %s\n Result : %s",
                operationItem,
                "Already initialized"
            )
            return
        } else {
            try {
                val res = wepinLogin.init()
//                                res.whenComplete()
                res?.whenComplete { infResponse, error ->
                    if (error == null) {
                        // render logged in UI
                        println(infResponse)
                        tvResult?.text = String.format(
                            " Item : %s\n Result : %s",
                            operationItem,
                            infResponse
                        )
                    } else {
                        // render error UI
                        println(error)
                        tvResult?.text = String.format(
                            " Item : %s\n Result : %s",
                            operationItem,
                            "fail $error.message"
                        )
                    }
                }
                println("res - $res")
            } catch (e: Exception) {
                tvResult?.text = String.format(
                    " Item : %s\n Result : %s",
                    operationItem,
                    "fail"
                )
            }

        }
    }

    private fun <T, U> loginWepinProcess(operationItem:String, loginOption:T, loginFunction: (T) -> CompletableFuture<U>, tokenType: OauthTokenType? = null) {
        try{
            loginFunction(loginOption).whenComplete { loginResponse, error ->
                if (error == null) {
                    // render logged in UI
                    println(loginResponse)
                    when(tokenType){
                        OauthTokenType.ID_TOKEN -> loginIdToken(operationItem, (loginResponse as LoginOauthResult).token)
                        OauthTokenType.ACCESS_TOKEN -> loginAccessToken(operationItem, (loginResponse as LoginOauthResult).provider, (loginResponse as LoginOauthResult).token)
                        else -> {
                            loginResult = loginResponse as LoginResult
                            tvResult?.text = String.format(
                                " Item : %s\n Result : %s",
                                operationItem,
                                loginResponse
                            )
                        }
                    }
                } else {
                    println("login error - ${error.message}")
                    // render error UI
                    tvResult?.text = String.format(
                        " Item : %s\n Result : %s",
                        operationItem,
                        "fail - ${error.message}"
                    )
                }
            }
        }catch (e: Exception) {
            println("Login Failed: ${e.message}")
            tvResult?.text = String.format(
                " Item : %s\n Result : %s",
                operationItem,
                "fail"
            )
        }
    }

    private fun wepinLoginIsInitialized(operationItem: String, isInitialized:Boolean? = false):Boolean{
        val isInit: Boolean = wepinLogin.isInitialized()
        if(!isInit) {
            tvResult?.text = String.format(
                " Item : %s\n Result : %s",
                operationItem,
                "is NOT initialized"
            )
        } else if (isInitialized == true) {
            tvResult?.text = String.format(
                " Item : %s\n Result : %s",
                operationItem,
                "initialized"
            )
        } else {
            tvResult?.text = String.format(
                " Item : %s\n Processing...",
                operationItem,
            )
        }
        return isInit
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LoginLibraryTheme {
        Greeting("Android")
    }
}