package com.wepin.android.loginlib.manager

import com.wepin.android.commonlib.types.WepinUser
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import java.util.concurrent.CompletableFuture

internal class CompletableFutureManager {
    private var loginOauthFuture: CompletableFuture<LoginOauthResult> = CompletableFuture()
    private var loginFuture: CompletableFuture<LoginResult> = CompletableFuture()
    private var loginWepinFuture: CompletableFuture<WepinUser> = CompletableFuture()

    fun resetFutures() {
        loginOauthFuture = CompletableFuture()
        loginFuture = CompletableFuture()
        // WepinLoginFuture도 함께 초기화
        loginWepinFuture = CompletableFuture()
    }

    // OAuth 관련
    fun completeOAuthSuccess(result: LoginOauthResult) {
        loginOauthFuture.complete(result)
    }

    fun completeOAuthError(error: Exception) {
        loginOauthFuture.completeExceptionally(error)
    }

    fun getOAuthFuture(): CompletableFuture<LoginOauthResult> = loginOauthFuture

    // Login 관련
    fun completeLoginSuccess(result: LoginResult) {
        loginFuture.complete(result)
    }

    fun completeLoginError(error: Exception) {
        loginFuture.completeExceptionally(error)
    }

    fun completeLoginError(error: Throwable) {
        loginFuture.completeExceptionally(error)
    }

    fun getLoginFuture(): CompletableFuture<LoginResult> = loginFuture

    // WepinLogin 관련
    fun completeWepinUserSuccess(result: WepinUser) {
        loginWepinFuture.complete(result)
    }

    fun completeWepinUserError(error: Exception) {
        loginWepinFuture.completeExceptionally(error)
    }

    fun completeWepinUserError(error: Throwable) {
        loginWepinFuture.completeExceptionally(error)
    }

    fun getWepinUserFuture(): CompletableFuture<WepinUser> = loginWepinFuture
} 