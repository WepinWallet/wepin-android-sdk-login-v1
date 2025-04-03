package com.wepin.android.loginlib.manager

import com.wepin.android.commonlib.WepinCommon
import com.wepin.android.commonlib.types.WepinUser
import com.wepin.android.loginlib.appAuth.LoginHelper
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.networklib.WepinFirebase
import com.wepin.android.networklib.WepinNetwork
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

internal class WepinLoginManager private constructor() {
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

    companion object {
        @Volatile
        private var instance: WepinLoginManager? = null

        fun getInstance(): WepinLoginManager =
            instance ?: synchronized(this) {
                instance ?: WepinLoginManager().also { instance = it }
            }
    }

    fun init(appKey: String, appId: String) {
        _appKey = appKey
        _appId = appId

        // Get the already initialized WepinNetwork instance
        wepinNetwork = WepinNetwork.getInstance()
        wepinFirebase = WepinFirebase.getInstance()

        loginHelper = LoginHelper(wepinNetwork!!, wepinFirebase!!, completableFutureManager)
        loginHelper?.init(buildRedirectUrl(appKey, appId))
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