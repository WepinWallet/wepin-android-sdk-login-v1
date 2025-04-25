package com.wepin.android.loginlib.manager

import android.content.Context
import com.wepin.android.commonlib.WepinCommon
import com.wepin.android.commonlib.types.WepinUser
import com.wepin.android.core.WepinCoreManager
import com.wepin.android.core.network.WepinFirebase
import com.wepin.android.core.network.WepinNetwork
import com.wepin.android.core.utils.Log
import com.wepin.android.loginlib.appAuth.LoginHelper
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

internal class WepinLoginManager {
    private val TAG = this.javaClass.name

    //    private var contextRef: WeakReference<Context>? = null
    internal var wepinNetwork: WepinNetwork? = null
    internal var wepinFirebase: WepinFirebase? = null
    private var _appKey: String? = null
    private var _appId: String? = null
    private var _packageName: String? = null
    private var _version: String? = null
    private val completableFutureManager = CompletableFutureManager()
    internal var loginHelper: LoginHelper? = null
    internal val redirectUrl: String
        get() = loginHelper?.appAuthRedirectUrl ?: throw IllegalStateException("Not initialized")

    fun init(
        context: Context,
        appKey: String,
        appId: String,
        platform: String = "android"
    ): CompletableFuture<Unit> {
        Log.d(TAG, "loginManager init")
//        val sdkType = "${platform}-login"
        val initFuture = CompletableFuture<Unit>()

        val sdkType = "$platform-login"

        WepinCoreManager.initialize(context, appId, appKey, platform, sdkType)
            .thenAccept {
                wepinNetwork = WepinCoreManager.getNetwork()
                wepinFirebase = WepinCoreManager.getFirebase()

                loginHelper = LoginHelper(wepinNetwork!!, wepinFirebase!!, completableFutureManager)
                loginHelper?.init(buildRedirectUrl(appKey, appId))

                initFuture.complete(Unit)
            }.exceptionally { error ->
                initFuture.completeExceptionally(error)
                null
            }

        return initFuture
    }

    fun initCompletableFuture() {
        completableFutureManager.resetFutures()
    }

    private fun buildRedirectUrl(appKey: String, appId: String): String {
        val baseUrl = WepinCommon.getWepinSdkUrl(appKey).get("sdkBackend")
        val redirectPath = "wepin.$appId:/oauth2redirect"
        val encodedRedirectPath = URLEncoder.encode(redirectPath, StandardCharsets.UTF_8.toString())
        return "${baseUrl}user/oauth/callback?uri=$encodedRedirectPath"
    }

    fun getLoginOAuthFuture(): CompletableFuture<LoginOauthResult> {
        return completableFutureManager.getOAuthFuture()
    }

    fun getLoginFuture(): CompletableFuture<LoginResult> {
        return completableFutureManager.getLoginFuture()
    }

    fun getWepinUserFuture(): CompletableFuture<WepinUser> {
        return completableFutureManager.getWepinUserFuture()
    }

    fun clear() {
        WepinCoreManager.clear()
        wepinNetwork = null
        wepinFirebase = null
        loginHelper = null
        _appKey = null
        _appId = null
        _packageName = null
        _version = null
        completableFutureManager.resetFutures()
    }
}