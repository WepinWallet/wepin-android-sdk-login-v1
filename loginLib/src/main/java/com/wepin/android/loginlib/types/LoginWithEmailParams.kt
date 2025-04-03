package com.wepin.android.loginlib.types

data class LoginWithEmailParams(
    val email: String,
    val password: String,
    val locale: String? = "en"
)
