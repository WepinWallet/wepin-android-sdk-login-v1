package com.wepin.android.loginlib.types.network

//sealed class CheckEmailExistResponse
//
//data class CheckEmailExistSuccess(
//    val isEmailExist: Boolean,
//    val isEmailVerified: Boolean,
//    val providerIds: List<String>
//) :CheckEmailExistResponse()
//
//data class CheckEmailExistError(val error: ErrorResponse) :CheckEmailExistResponse()

internal data class CheckEmailExistResponse(
    val isEmailExist: Boolean,
    val isEmailverified: Boolean,
    val providerIds: List<String>
)