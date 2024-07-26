package com.wepin.android.loginlib.types.network.firebase

internal data class FirebaseAuthError(
    val error: FirebaseError
)

internal data class FirebaseError(
    val code: Int,
    val message: String
)