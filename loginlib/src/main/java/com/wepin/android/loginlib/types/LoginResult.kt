package com.wepin.android.loginlib.types

import com.wepin.android.commonlib.types.Providers

data class FBToken(
    val idToken: String,
    val refreshToken: String
)

data class LoginResult(
    val provider: Providers,
    val token: FBToken
)