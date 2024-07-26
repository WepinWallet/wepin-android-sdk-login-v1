package com.wepin.android.loginlib.types.network.firebase

internal sealed class GetCurrentUserResponse

internal data class GetCurrentUserSuccess(
    val users: List<UserInfo>
) : GetCurrentUserResponse()

internal data class GetCurrentUserError(val error: FirebaseAuthError) :  GetCurrentUserResponse()

internal data class UserInfo(
    val localId: String,
    val email: String,
    val emailVerified: Boolean,
    val displayName: String,
    val providerUserInfo: List<ProviderUserInfo>,
    val photoUrl: String,
    val passwordHash: String,
    val passwordUpdatedAt: Any?,
    val validSince: String,
    val disabled: Boolean,
    val lastLoginAt: String,
    val createdAt: String,
    val customAuth: Boolean
)

internal data class ProviderUserInfo(
    val providerId: String,
    val displayName: String?,
    val photoUrl: String?,
    val federatedId: String,
    val email: String?,
    val rawId: String?,
    val screenName: String?
)
