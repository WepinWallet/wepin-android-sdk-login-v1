package com.wepin.android.loginlib.types.network

internal data class VerifyRequest (
    val type: String,
    val email: String,
    val localeId: Int? = 1
)