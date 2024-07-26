package com.wepin.android.loginlib.types

import com.google.gson.annotations.SerializedName

enum class Oauth2Providers {
    @SerializedName("google")
    GOOGLE,
    @SerializedName("apple")
    APPLE,
    @SerializedName("naver")
    NAVER,
    @SerializedName("discord")
    DISCORD,
}