package com.wepin.android.loginlib.network

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.wepin.android.loginlib.network.WepinNetworkManager.Companion.getErrorMessage
import com.wepin.android.loginlib.types.network.firebase.EmailAndPasswordRequest
import com.wepin.android.loginlib.types.network.NetworkJsonObjectRequest
import com.wepin.android.loginlib.types.network.firebase.GetCurrentUserRequest
import com.wepin.android.loginlib.types.network.firebase.GetCurrentUserResponse
import com.wepin.android.loginlib.types.network.firebase.GetRefreshIdTokenRequest
import com.wepin.android.loginlib.types.network.firebase.GetRefreshIdTokenSuccess
import com.wepin.android.loginlib.types.network.firebase.ResetPasswordRequest
import com.wepin.android.loginlib.types.network.firebase.ResetPasswordResponse
import com.wepin.android.loginlib.types.network.firebase.SignInResponse
import com.wepin.android.loginlib.types.network.firebase.SignInWithCustomTokenSuccess
import com.wepin.android.loginlib.types.network.firebase.UpdatePasswordSuccess
import com.wepin.android.loginlib.types.network.firebase.VerifyEmailRequest
import com.wepin.android.loginlib.types.network.firebase.VerifyEmailResponse
import com.wepin.android.loginlib.utils.convertToJsonRequest
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


internal class WepinFirebaseManager(context:Context, firebaseKey:String) {
    private val TAG = this.javaClass.name
    private val firebaseUrl: String = "https://identitytoolkit.googleapis.com/v1/"
    private var executorService: ExecutorService? = null
    private var _context: Context? = null
    private var requestQueue: RequestQueue
    private var firebaseKey: String

    init {
        this.firebaseKey = firebaseKey
        executorService = Executors.newSingleThreadExecutor()
        this._context = context
        requestQueue = Volley.newRequestQueue(context)
    }



    fun signInWithCustomToken(customToken:String) : CompletableFuture<SignInWithCustomTokenSuccess> {
        val future: CompletableFuture<SignInWithCustomTokenSuccess> = CompletableFuture<SignInWithCustomTokenSuccess>()
        executorService?.submit {
            val url = firebaseUrl + "accounts:signInWithCustomToken?key=$firebaseKey"
            val jsonRequestBody = JSONObject().apply {
                put("token", customToken)
                put("returnSecureToken", true)
            }
            val jsonObjectRequest = NetworkJsonObjectRequest<SignInWithCustomTokenSuccess>(
                Request.Method.POST, url, jsonRequestBody,
                SignInWithCustomTokenSuccess::class.java,
                { loginResponse ->
                    // POST 요청 성공 처리
                    Log.d(TAG,"POST loginResponse: $loginResponse")
                    if(loginResponse is SignInWithCustomTokenSuccess) {
                        future.complete(loginResponse)
                    }else {
                        future.completeExceptionally(Error("invalid response"))
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

    fun signInWithEmailPassword(signInRequest: EmailAndPasswordRequest) : CompletableFuture<SignInResponse> {
        val future: CompletableFuture<SignInResponse> = CompletableFuture<SignInResponse>()
        executorService?.submit {
            val url = firebaseUrl + "accounts:signInWithPassword?key=$firebaseKey"
            val jsonRequestBody = convertToJsonRequest(signInRequest)
            val jsonObjectRequest = NetworkJsonObjectRequest<SignInResponse>(
                Request.Method.POST, url, jsonRequestBody,
                SignInResponse::class.java,
                { signInResponse ->
                    // POST 요청 성공 처리
                    Log.d(TAG, "POST signInResponse: $signInResponse")
                    if(signInResponse is SignInResponse) {
                        future.complete(signInResponse)
                    }else {
                        future.completeExceptionally(Error("invalid response"))
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

    fun getCurrentUser(getCurrentUserRequest: GetCurrentUserRequest) : CompletableFuture<GetCurrentUserResponse> {
        val future: CompletableFuture<GetCurrentUserResponse> = CompletableFuture<GetCurrentUserResponse>()
        executorService?.submit {
            val url = firebaseUrl + "accounts:lookup?key=$firebaseKey"
            val jsonRequestBody = convertToJsonRequest(getCurrentUserRequest)
            val jsonObjectRequest = NetworkJsonObjectRequest<GetCurrentUserResponse>(
                Request.Method.POST, url, jsonRequestBody,
                GetCurrentUserResponse::class.java,
                { getCurrentUserResponse ->
                    // POST 요청 성공 처리
                    Log.d(TAG,"POST getCurrentUserResponse: $getCurrentUserResponse")
                    if(getCurrentUserResponse is GetCurrentUserResponse) {
                        future.complete(getCurrentUserResponse)
                    }else {
                        future.completeExceptionally(Error("invalid response"))
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

    fun getRefreshIdToken(getRefreshIdTokenRequest: GetRefreshIdTokenRequest) : CompletableFuture<GetRefreshIdTokenSuccess> {
        val future: CompletableFuture<GetRefreshIdTokenSuccess> = CompletableFuture<GetRefreshIdTokenSuccess>()
        executorService?.submit {
            val url = firebaseUrl + "token?key=$firebaseKey"
            val jsonRequestBody = convertToJsonRequest(getRefreshIdTokenRequest)
            val jsonObjectRequest = NetworkJsonObjectRequest<GetRefreshIdTokenSuccess>(
                Request.Method.POST, url, jsonRequestBody,
                GetRefreshIdTokenSuccess::class.java,
                { getRefreshIdTokenResponse ->
                    // POST 요청 성공 처리
                    Log.d(TAG,"POST getRefreshIdTokenResponse: $getRefreshIdTokenResponse")
                    if(getRefreshIdTokenResponse is GetRefreshIdTokenSuccess) {
                        future.complete(getRefreshIdTokenResponse)
                    }else {
                        future.completeExceptionally(Error("invalid response"))
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

    fun resetPassword(resetPasswordRequest: ResetPasswordRequest) : CompletableFuture<ResetPasswordResponse> {
        val future: CompletableFuture<ResetPasswordResponse> = CompletableFuture<ResetPasswordResponse>()
        executorService?.submit {
            val url = firebaseUrl + "accounts:resetPassword?key=$firebaseKey"
            val jsonRequestBody = convertToJsonRequest(resetPasswordRequest)
            val jsonObjectRequest = NetworkJsonObjectRequest<ResetPasswordResponse>(
                Request.Method.POST, url, jsonRequestBody,
                ResetPasswordResponse::class.java,
                { resetPasswordResponse ->
                    // POST 요청 성공 처리
                    Log.d(TAG,"POST resetPasswordResponse: $resetPasswordResponse")
                    if(resetPasswordResponse is ResetPasswordResponse) {
                        future.complete(resetPasswordResponse)
                    }else {
                        future.completeExceptionally(Error("invalid response"))
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

    fun verifyEmail(verifyEmailRequest: VerifyEmailRequest) : CompletableFuture<VerifyEmailResponse> {
        val future: CompletableFuture<VerifyEmailResponse> = CompletableFuture<VerifyEmailResponse>()
        executorService?.submit {
            val url = firebaseUrl + "accounts:update?key=$firebaseKey"
            val jsonRequestBody = convertToJsonRequest(verifyEmailRequest)
            val jsonObjectRequest = NetworkJsonObjectRequest<VerifyEmailResponse>(
                Request.Method.POST, url, jsonRequestBody,
                VerifyEmailResponse::class.java,
                { verifyEmailResponse ->
                    // POST 요청 성공 처리
                    Log.d(TAG,"POST verifyEmailResponse: $verifyEmailResponse")
                    if(verifyEmailResponse is VerifyEmailResponse) {
                        future.complete(verifyEmailResponse)
                    }else {
                        future.completeExceptionally(Error("invalid response"))
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

    fun updatePassword(idToken:String, password:String) :CompletableFuture<UpdatePasswordSuccess>{
        val future: CompletableFuture<UpdatePasswordSuccess> = CompletableFuture<UpdatePasswordSuccess>()
        executorService?.submit {
            val url = firebaseUrl + "accounts:update?key=$firebaseKey"
            val jsonRequestBody = JSONObject().apply {
                put("idToken", idToken)
                put("password", password)
                put("returnSecureToken", true)
            }
            val jsonObjectRequest = NetworkJsonObjectRequest<UpdatePasswordSuccess>(
                Request.Method.POST, url, jsonRequestBody,
                UpdatePasswordSuccess::class.java,
                { verifyEmailResponse ->
                    // POST 요청 성공 처리
                    Log.d(TAG,"POST verifyEmailResponse: $verifyEmailResponse")
                    if(verifyEmailResponse is UpdatePasswordSuccess) {
                        future.complete(verifyEmailResponse)
                    }else {
                        future.completeExceptionally(Error("invalid response"))
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
}