package com.wepin.android.loginlib.types.network

internal data class PasswordStateResponse(var isPasswordResetRequired: Boolean)
internal data class PasswordStateRequest(var isPasswordResetRequired: Boolean)