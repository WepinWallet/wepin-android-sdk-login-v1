package com.wepin.android.loginlib.types.network

internal data class VerifyResponse(
    val result: Boolean,
    val oobReset:String?,
    val oobVerify:String?
)
