package com.wepin.android.loginlib.types.network


internal data class LoginOauthIdTokenResponse(
    val result: Boolean,
    val token: String? = null,
    val signVerifyResult: Boolean?,
    val error: String? = null
)