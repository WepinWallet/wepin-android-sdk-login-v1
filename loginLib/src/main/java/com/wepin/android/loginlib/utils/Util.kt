package com.wepin.android.loginlib.utils

import android.content.pm.PackageManager
import com.wepin.android.loginlib.BuildConfig
import org.mindrot.jbcrypt.BCrypt

fun getVersionMetaDataValue(): String {
    try {
        return BuildConfig.LIBRARY_VERSION
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    return ""
}

fun hashPassword(password: String): String {
    val BCRYPT_SALT = "\$2a\$10\$QCJoWqnN.acrjPIgKYCthu"
    return BCrypt.hashpw(password, BCRYPT_SALT)
}