package com.wepin.android.loginlib.const

import android.net.Uri

internal class AppAuthConst {
    companion object {
        fun getAuthorizationEndpoint(provider: String): Uri {
            return when (provider) {
                "google" -> Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
                "apple" -> Uri.parse("https://appleid.apple.com/auth/authorize")
                "discord" -> Uri.parse("https://discord.com/api/oauth2/authorize")
                "naver" -> Uri.parse("https://nid.naver.com/oauth2.0/authorize")
                else -> Uri.parse("")
            }
        }

        fun getTokenEndpoint(provider: String): Uri {
            return when (provider) {
                "google" -> Uri.parse("https://oauth2.googleapis.com/token")
                "apple" -> Uri.parse("https://appleid.apple.com/auth/token")
                "discord" -> Uri.parse("https://discord.com/api/oauth2/token")
                "naver" -> Uri.parse("https://nid.naver.com/oauth2.0/token")
                else -> Uri.parse("")
            }
        }
    }
}