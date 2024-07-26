package com.wepin.android.loginlib.types.network.firebase

//sealed class VerifyEmailResponse
//
//data class VerifyEmailSuccess (
//    var localId: String,
//    var email: String,
//    var passwordHash: String,
//    var providerUserInfo: VerifyProviderUserInfo
//) : VerifyEmailResponse()
//data class VerifyEmailError(val error: FirebaseAuthError) :  VerifyEmailResponse()
//


internal data class VerifyEmailResponse (
    val localId: String,
    val email: String,
    val passwordHash: String,
    val providerUserInfo: List<ProviderUserInfo>
)
