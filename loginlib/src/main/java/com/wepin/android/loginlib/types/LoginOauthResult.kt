package com.wepin.android.loginlib.types

data class LoginOauthResult(
    val provider: String,
    val token: String,
    val type: OauthTokenType
)

enum class OauthTokenType(val value: String) {
    ID_TOKEN("id_token"),
    ACCESS_TOKEN("accessToken")
}
