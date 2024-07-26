package com.wepin.android.loginlib.types.network

sealed class AppInfoResponse<out T> {
    data class Success<out T>(val data: T) : AppInfoResponse<T>()
    data class Error(val error: ErrorResponse) : AppInfoResponse<ErrorResponse>()
}