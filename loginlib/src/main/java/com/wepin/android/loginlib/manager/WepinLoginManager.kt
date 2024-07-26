package com.wepin.android.loginlib.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.wepin.android.loginlib.appAuth.LoginHelper
import com.wepin.android.loginlib.network.WepinFirebaseManager
import com.wepin.android.loginlib.network.WepinNetworkManager
import com.wepin.android.loginlib.types.LoginOauthResult
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.WepinUser
import com.wepin.android.loginlib.utils.getVersionMetaDataValue
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

internal class WepinLoginManager () {
    private var _contex: Context?  = null
    var wepinNewtorkManager: WepinNetworkManager? = null
    var wepinFirebaseManager: WepinFirebaseManager? = null
    private var _appKey: String? = null
    private var _appId: String? = null
    private var _packageName: String? = null
    private var _version: String? = null
    internal var loginCompletableFuture: CompletableFuture<LoginResult> = CompletableFuture()
    internal var loginOauthCompletableFuture: CompletableFuture<LoginOauthResult> = CompletableFuture()
    internal var loginWepinCompletableFutre: CompletableFuture<WepinUser> = CompletableFuture<WepinUser>()
    internal var appAuthRedirectUrl: String = ""
    internal var loginHelper: LoginHelper? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var _instance: WepinLoginManager? = null
        fun getInstance(): WepinLoginManager {
            if (null == _instance) {
                _instance = WepinLoginManager()
            }
            return _instance as WepinLoginManager
        }
    }

    fun init(context:Context, appKey: String, appId: String) {
        _contex = context
        _version = getVersionMetaDataValue()
        _packageName = (context as Activity).packageName
        _appKey = appKey
        _appId = appId
        wepinNewtorkManager = WepinNetworkManager(_contex as Activity, _appKey!!, _packageName!!, _version!!)
        appAuthRedirectUrl = "${wepinNewtorkManager?.wepinBaseUrl}user/oauth/callback?uri=${URLEncoder.encode("wepin.$_appId:/oauth2redirect", StandardCharsets.UTF_8.toString())}"
        loginHelper = LoginHelper(this)
        initLoginCompletableFuture()
    }

    fun setFirebase(key:String) {
        wepinFirebaseManager = _contex?.let { WepinFirebaseManager(it, key) }
    }

    fun initLoginCompletableFuture() {
        loginCompletableFuture = CompletableFuture<LoginResult>()
        loginOauthCompletableFuture = CompletableFuture<LoginOauthResult>()
        loginWepinCompletableFutre = CompletableFuture<WepinUser>()
    }
}
