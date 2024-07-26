package com.wepin.android.loginlib.types.network.firebase

internal data class UpdatePasswordRequest(
    var idToken : String,
    var password: String,
    var returnSecureToken: Boolean
)
