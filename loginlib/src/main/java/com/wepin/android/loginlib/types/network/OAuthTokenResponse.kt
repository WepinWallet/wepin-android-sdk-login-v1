package com.wepin.android.loginlib.types.network

internal data class OAuthTokenResponse(
    val id_token: String?,
    val access_token: String,
    val token_type:String,
    val expires_in:String?,
    val refresh_token: String?,
    val scope: String?,
)
