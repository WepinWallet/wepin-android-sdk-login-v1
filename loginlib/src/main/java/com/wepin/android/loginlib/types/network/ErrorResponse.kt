package com.wepin.android.loginlib.types.network

data class ErrorResponse(
    val statusCode: Int,
    val status: Int? = null,
    val timestamp: String,
    val path: String,
    val message: String,
    val remainPinTryCnt: Int? = null,
    val code: Int,
    val validationError: String? = null
)
