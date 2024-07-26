package com.wepin.android.loginlib.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.wepin.android.loginlib.error.WepinError
import com.wepin.android.loginlib.types.KeyType
import com.wepin.android.loginlib.types.network.AppInfoResponse
import com.wepin.android.loginlib.types.network.CheckEmailExistResponse
import com.wepin.android.loginlib.types.network.GetAccessTokenResponse
import com.wepin.android.loginlib.types.network.LoginOauthAccessTokenRequest
import com.wepin.android.loginlib.types.network.LoginOauthIdTokenRequest
import com.wepin.android.loginlib.types.network.LoginOauthIdTokenResponse
import com.wepin.android.loginlib.types.network.LoginRequest
import com.wepin.android.loginlib.types.network.LoginResponse
import com.wepin.android.loginlib.types.network.OAuthTokenRequest
import com.wepin.android.loginlib.types.network.OAuthTokenResponse
import com.wepin.android.loginlib.types.network.PasswordStateRequest
import com.wepin.android.loginlib.types.network.PasswordStateResponse
import com.wepin.android.loginlib.types.network.VerifyRequest
import com.wepin.android.loginlib.types.network.VerifyResponse
import com.wepin.android.loginlib.types.network.WepinJsonObjectRequest
import com.wepin.android.loginlib.types.network.WepinStringObjectRequest
import com.wepin.android.loginlib.utils.convertToJsonRequest
import com.wepin.android.loginlib.utils.decodeBase64
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


internal class WepinNetworkManager(context: Context, appKey:String, domain:String, version: String) {
    private val TAG = this.javaClass.name
    internal var wepinBaseUrl: String? = null
    private var appKey: String? = null
    private var domain: String? = null
    private var version: String? = null
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var executorService: ExecutorService? = null
    private var _context: Context? = null
    private var requestQueue: RequestQueue
    
    init {
        wepinBaseUrl = getSdkUrl(appKey)
        this.appKey = appKey
        this.domain = domain
        this.version = version
        this._context = context
        WepinJsonObjectRequest.setHeaderInfo(domain, appKey, version)
        WepinStringObjectRequest.setHeaderInfo(domain, appKey, version)
        executorService = Executors.newSingleThreadExecutor();
        requestQueue = Volley.newRequestQueue(context)
    }
    companion object {
        @SuppressLint("ObsoleteSdkInt")
        fun isInternetAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                return when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                return activeNetworkInfo != null && activeNetworkInfo.isConnected
            }
        }
        internal fun getErrorMessage(volleyError: VolleyError) : String? {
            val networkResponse = volleyError.networkResponse
            if (networkResponse != null) {
                val statusCode = networkResponse.statusCode
                val errorMessage = String(networkResponse.data)
                Log.e("Volley Error", "Status Code: $statusCode, Error Message: $errorMessage")
                return "Status Code: $statusCode, Error Message: $errorMessage"
            }
            return null
        }
    }

    internal fun setAuthToken(accessToken:String, refreshToken:String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }
    internal fun clearAuthToken(){
        this.accessToken = null
        this.refreshToken = null
    }

    fun getAppInfo() : CompletableFuture<Boolean> {
        val future: CompletableFuture<Boolean> = CompletableFuture<Boolean>()
        executorService?.submit {
            val url = wepinBaseUrl + "app/info"
//            val jsonRequestBody = convertToJsonRequest(verifyEmailRequest)
            val jsonObjectRequest = WepinJsonObjectRequest<JSONObject>(
                Request.Method.GET, url, null,
                JSONObject::class.java,
                null,
                { response ->
                    // GET 요청 성공 처리
                    Log.d(TAG,"GET getAppInfo: $response")

                    if(response != null) {
                        AppInfoResponse.Success(response)
                        future.complete(true)
                    }else {
                        future.complete(false)
                    }
                },
                { error ->
                    // GET 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if(message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future;
    }

    fun getFirebaseConfig() : CompletableFuture<String> {
        val future: CompletableFuture<String> = CompletableFuture<String>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/firebase-config"
//            val jsonRequestBody = convertToJsonRequest(verifyEmailRequest)
            val stringRequest = WepinStringObjectRequest(
                Request.Method.GET, url, null, null,
                { response ->
                    // GET 요청 성공 처리
                    Log.d(TAG,"GET getFirebaseConfig: $response")
                    val decodeString = decodeBase64(response)
                    val jsonObject = JSONObject(decodeString)
                    val key = jsonObject.getString("apiKey")
                    future.complete(key)
                },
                { error ->
                    // GET 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if(message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(stringRequest)
        }
        return future;
    }

    fun login(idToken: String) : CompletableFuture<LoginResponse> {
        val loginRequest = LoginRequest(idToken)
        val future: CompletableFuture<LoginResponse> = CompletableFuture<LoginResponse>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/login"
            val jsonRequestBody = convertToJsonRequest(loginRequest)
            val jsonObjectRequest = WepinJsonObjectRequest<LoginResponse>(
                Request.Method.POST, url, jsonRequestBody,
                LoginResponse::class.java,
                null,
                { response ->
                    // POST 요청 성공 처리
                    Log.d(TAG, "Login Success: $response")
                    setAuthToken(response.token.access, response.token.refresh)
                    future.complete(response)
//                        else future.completeExceptionally(Exception("loginOAuthIdToken result fail"))
                },
                { error ->
                    // POST 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if (message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future
    }

    fun logout(userId: String ): CompletableFuture<Boolean>{
        val future: CompletableFuture<Boolean> = CompletableFuture<Boolean>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/${userId}/logout"
//            val jsonRequestBody = convertToJsonRequest(verifyEmailRequest)
            val headers = HashMap<String, String>()
            headers["Authorization"] = "Bearer $accessToken"
            val jsonObjectRequest = WepinJsonObjectRequest<JSONObject>(
                Request.Method.POST, url, null,
                JSONObject::class.java,
                headers,
                { response ->
                    // POST 요청 성공 처리
                    Log.d(TAG,"POST logout: $response")
                    clearAuthToken()
                    if(response != null) {
                        future.complete(true)
                    }else {
                        future.complete(false)
                    }
                },
                { error ->
                    // POST 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if(message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future;
    }

    fun getAccessToken(userId: String): CompletableFuture<String>{
        val future: CompletableFuture<String> = CompletableFuture<String>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/access-token?userId=${userId}&refresh_token=${this.refreshToken}"
            val headers = HashMap<String, String>()
            headers["Authorization"] = "Bearer $accessToken"
            val jsonObjectRequest = WepinJsonObjectRequest<GetAccessTokenResponse>(
                Request.Method.GET, url, null,
                GetAccessTokenResponse::class.java,
                headers,
                { response ->
                    // GET 요청 성공 처리
                    Log.d(TAG,"GET getAccessToken: $response")
                    if(response != null) {
                        setAuthToken(response.token, this.refreshToken!!)
                        future.complete(response.token)
                    }else {
                        future.completeExceptionally(WepinError.INVALID_TOKEN)
                    }
                },
                { error ->
                    // GET 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if(message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future;
    }

    fun loginOAuthIdToken(params:LoginOauthIdTokenRequest) : CompletableFuture<LoginOauthIdTokenResponse>{
        val future: CompletableFuture<LoginOauthIdTokenResponse> = CompletableFuture<LoginOauthIdTokenResponse>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/oauth/login/id-token"
            val jsonRequestBody = convertToJsonRequest(params)
            val jsonObjectRequest = WepinJsonObjectRequest<LoginOauthIdTokenResponse>(
                Request.Method.POST, url, jsonRequestBody,
                LoginOauthIdTokenResponse::class.java,
                null,
                { response ->
                    // POST 요청 성공 처리
                    Log.d(TAG, "loginOAuthIdToken Success: $response")
                    val result = response.result
                    if(result) future.complete(response)
                    else future.completeExceptionally(Exception("loginOAuthIdToken result fail"))
//                        else future.completeExceptionally(Exception("loginOAuthIdToken result fail"))
                },
                { error ->
                    // POST 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if (message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future;
    }

    fun loginOAuthAccessToken(params: LoginOauthAccessTokenRequest) : CompletableFuture<LoginOauthIdTokenResponse>{
        val future: CompletableFuture<LoginOauthIdTokenResponse> = CompletableFuture<LoginOauthIdTokenResponse>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/oauth/login/access-token"
            val jsonRequestBody = convertToJsonRequest(params)
            val jsonObjectRequest = WepinJsonObjectRequest<LoginOauthIdTokenResponse>(
                Request.Method.POST, url, jsonRequestBody,
                LoginOauthIdTokenResponse::class.java,
                null,
                { response ->
                    // POST 요청 성공 처리
                    Log.d(TAG, "loginOAuthIdToken Success: $response")
                    val result = response.result
                    if(result) future.complete(response)
                    else future.completeExceptionally(Exception("loginOAuthAccessToken result fail"))
                },
                { error ->
                    // POST 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if (message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future;
    }

    fun checkEmailExist(email: String) : CompletableFuture<CheckEmailExistResponse> {
        val future: CompletableFuture<CheckEmailExistResponse> = CompletableFuture<CheckEmailExistResponse>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/check-user?email=$email"
//            val jsonRequestBody = convertToJsonRequest(params)
            val jsonObjectRequest = WepinJsonObjectRequest<CheckEmailExistResponse>(
                Request.Method.GET, url, null,
                CheckEmailExistResponse::class.java,
                null,
                { response ->
                    // GET 요청 성공 처리
                    Log.d(TAG, "checkEmailExist Success: $response")
                    future.complete(response)
                },
                { error ->
                    // GET 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if (message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future
    }

    // JAVA와의 호환성을 위해.. 코루틴대신에 CompletableFuture를 사용하자....
//    @OptIn(ExperimentalCoroutinesApi::class)
//    suspend fun checkEmailExist(email: String) : CheckEmailExistResponse {
//        return withContext(Dispatchers.IO) {
//            suspendCancellableCoroutine { continuation ->
//                Log.d("WepinNetworkManager", "checkEmailExist")
//                Log.d("WepinNetworkManager", "checkEmailExist $email")
//                val call = wepinApiService!!.checkEmailExist(email) // API 호출을 위한 Retrofit call 객체 생성
//                call?.enqueue(object : Callback<CheckEmailExistResponse?> {
//                    override fun onResponse(
//                        call: Call<CheckEmailExistResponse?>,
//                        response: Response<CheckEmailExistResponse?>
//                    ) {
//                        if (response.isSuccessful) {
//                            val checkEmailExistResponse = response.body()
//                            Log.d("NetworkManager", "checkEmailExist Success: $checkEmailExistResponse")
//                            if (checkEmailExistResponse != null) {
//                                continuation.resume(checkEmailExistResponse)
//                            } else {
//                                continuation.resumeWithException(Exception("Response body is null"))
//                            }
//                        } else {
//                            Log.e("NetworkManager", "checkEmailExist Error: ${response.code()}")
//                            continuation.resumeWithException(Exception("HTTP error ${response.code()}"))
//                        }
//                    }
//
//                    override fun onFailure(call: Call<CheckEmailExistResponse?>, t: Throwable) {
//                        Log.e("NetworkManager", "checkEmailExist Failure: ${t.message}")
//                        continuation.resumeWithException(t)
//                    }
//                })
//
//                continuation.invokeOnCancellation {
//                    call?.cancel() // 코루틴이 취소되면 Retrofit call도 취소
//                }
//            }
//        }
//    }

    fun getUserPasswordState(email: String) : CompletableFuture<PasswordStateResponse> {
        val future: CompletableFuture<PasswordStateResponse> = CompletableFuture<PasswordStateResponse>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/password-state?email=$email"
//            val jsonRequestBody = convertToJsonRequest(params)
            val jsonObjectRequest = WepinJsonObjectRequest<PasswordStateResponse>(
                Request.Method.GET, url, null,
                PasswordStateResponse::class.java,
                null,
                { response ->
                    // GET 요청 성공 처리
                    Log.d(TAG, "getUserPasswordState Success: $response")
                    future.complete(response)
                },
                { error ->
                    // GET 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if (message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future
    }

    fun updateUserPasswordState(userId: String, passwordStateRequest: PasswordStateRequest) : CompletableFuture<PasswordStateResponse> {
        val future: CompletableFuture<PasswordStateResponse> = CompletableFuture<PasswordStateResponse>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/$userId/password-state"
            val headers = HashMap<String, String>()
            headers["Authorization"] = "Bearer $accessToken"
            val jsonRequestBody = convertToJsonRequest(passwordStateRequest)
            val jsonObjectRequest = WepinJsonObjectRequest<PasswordStateResponse>(
                Request.Method.PATCH, url, jsonRequestBody,
                PasswordStateResponse::class.java,
                headers,
                { response ->
                    // GET 요청 성공 처리
                    Log.d(TAG, "getUserPasswordState Success: $response")
                    future.complete(response)
                },
                { error ->
                    // GET 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if (message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future
    }

    fun verify(params: VerifyRequest) : CompletableFuture<VerifyResponse>{
        val future: CompletableFuture<VerifyResponse> = CompletableFuture<VerifyResponse>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/verify"
            val jsonRequestBody = convertToJsonRequest(params)
            val jsonObjectRequest = WepinJsonObjectRequest<VerifyResponse>(
                Request.Method.POST, url, jsonRequestBody,
                VerifyResponse::class.java,
                null,
                { response ->
                    // GET 요청 성공 처리
                    Log.d(TAG, "getUserPasswordState Success: $response")
                    future.complete(response)
                },
                { error ->
                    // GET 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if (message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future
    }

    fun oauthTokenRequest(provider:String, params: OAuthTokenRequest) : CompletableFuture<OAuthTokenResponse>{
        val future: CompletableFuture<OAuthTokenResponse> = CompletableFuture<OAuthTokenResponse>()
        executorService?.submit {
            val url = wepinBaseUrl + "user/oauth/token/$provider"
            val jsonRequestBody = convertToJsonRequest(params)
            val jsonObjectRequest = WepinJsonObjectRequest<OAuthTokenResponse>(
                Request.Method.POST, url, jsonRequestBody,
                OAuthTokenResponse::class.java,
                null,
                { response ->
                    // GET 요청 성공 처리
                    Log.d(TAG, "getUserPasswordState Success: $response")
                    future.complete(response)
                },
                { error ->
                    // GET 요청 실패 처리
                    error.printStackTrace()
                    val message = getErrorMessage(error)
                    if (message != null) future.completeExceptionally(Exception(message))
                    else future.completeExceptionally(error)
                }
            )
            // 요청 추가
            requestQueue.add(jsonObjectRequest)
        }
        return future
    }

    private fun getSdkUrl(apiKey:String): String {
        return when (KeyType.fromAppKey(apiKey)) {
            KeyType.DEV -> {
                "https://dev-sdk.wepin.io/v1/"
            }

            KeyType.STAGE-> {
                "https://stage-sdk.wepin.io/v1/"
            }

            KeyType.PROD -> {
                "https://sdk.wepin.io/v1/"
            }

            else -> {
                throw WepinError.INVALID_APP_KEY
            }
        }
    }


}