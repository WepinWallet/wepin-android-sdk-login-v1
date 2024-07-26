package com.wepin.android.loginlib.types.network.firebase


internal data class SignInResponse (
    var localId:String,
    var email:String,
    var displayName:String,
    var idToken:String,
    var registered:String,
    var refreshToken: String,
    var expiresIn:String,
)
