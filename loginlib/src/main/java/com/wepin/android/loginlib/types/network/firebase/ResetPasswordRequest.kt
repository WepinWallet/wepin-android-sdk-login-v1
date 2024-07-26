package com.wepin.android.loginlib.types.network.firebase

internal class ResetPasswordRequest(
    val oobCode: String,
    val newPassword: String,) {
}