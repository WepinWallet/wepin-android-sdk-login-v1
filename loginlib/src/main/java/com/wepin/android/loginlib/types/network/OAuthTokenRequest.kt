package com.wepin.android.loginlib.types.network

internal data class OAuthTokenRequest (
    val code: String,
    val state : String? = null,
    val clientId : String,
    val redirectUri: String,
    val codeVerifier: String? = null,
)