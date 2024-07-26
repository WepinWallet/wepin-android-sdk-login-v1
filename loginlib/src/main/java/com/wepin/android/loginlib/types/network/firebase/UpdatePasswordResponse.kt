package com.wepin.android.loginlib.types.network.firebase

internal data class UpdatePasswordSuccess (
    val kind: String?,
    val localId: String,
    val email: String,
    val displayName: String?,
    val passwordHash: String,
    val providerUserInfo: List<ProviderUserInfo>,
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String,
    val emailVerified: Boolean?,
)
