package com.wepin.android.loginlib.types.network.firebase

data class GetRefreshIdTokenRequest(
    var refresh_token:String,
    var grant_type: String = "refresh_token",
    ){
}