package com.wepin.android.loginlib.types

data class UserInfo(
    val userId: String,
    val email: String,
    val provider: Providers,
    val use2FA: Boolean
)

data class UserStatus(
    val loginStatus: WepinLoginStatus,
    val pinRequired: Boolean? = null
)

enum class WepinLoginStatus(val value:String) {
    COMPLETE("completed"),
    PIN_REQUIRED("pinRequired"),
    REGISTER_REQUIRED("registerRequired");
    companion object {
        fun fromValue(value: String): WepinLoginStatus? {
            return WepinLoginStatus.entries.find { it.value == value }
        }
    }
}

data class Token(
    val accessToken: String,
    val refreshToken: String
)

data class WepinUser(
    val status: String,
    val userInfo: UserInfo? = null,
    val walletId: String? = null,
    val userStatus: UserStatus? = null,
    val token: Token? = null
)
