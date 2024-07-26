package com.wepin.android.loginlib.types.network.firebase


internal data class SignInWithCustomTokenSuccess (
    var idToken:String,
    var refreshToken:String
)