package com.wepin.android.loginlib.types.network

data class LoginResponse(
    val loginStatus: String,
    val pinRequired: Boolean?,
    val walletId: String?,
    val token: Token,
    val userInfo: AppUser
)

data class Token(
    val refresh: String,
    val access: String
)
data class AppUser(
    val userId: String,
    val email: String,
    val name: String,
    val locale: String,
    val currency: String,
    val lastAccessDevice: String,
    val lastSessionIP: String,
    val userJoinStage: Int,
    val profileImage: String,
    val userState: Int,
    val use2FA: Int
){
    fun getUserJoinStageEnum(): UserJoinStage? {
        return UserJoinStage.fromStage(userJoinStage)
    }

    fun getUserState(): UserState? {
        return UserState.fromState(userState)
    }
}


enum class UserJoinStage(val stage: Int) {
    EMAIL_REQUIRE(1),
    PIN_REQUIRE(2),
    COMPLETE(3);

    companion object {
        fun fromStage(stage: Int): UserJoinStage? {
            return entries.find { it.stage == stage }
        }
    }
}

enum class UserState(val state: Int) {
    ACTIVE(1),
    DELETED(2);

    companion object {
        fun fromState(state: Int): UserState? {
            return entries.find { it.state == state }
        }
    }
}