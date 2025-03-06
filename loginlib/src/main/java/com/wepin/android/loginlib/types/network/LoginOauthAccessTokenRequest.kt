package com.wepin.android.loginlib.types.network

data class LoginOauthAccessTokenRequest(
    val provider: String,
    val accessToken: String,
    val sign: String? = null
)

enum class OauthAccessTokenProvider(val value: String) {
    NAVER("naver"),
    DISCORD("discord"),
}
