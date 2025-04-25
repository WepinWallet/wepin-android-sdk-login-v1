package com.wepin.android.loginlib.utils

import org.mindrot.jbcrypt.BCrypt

fun hashPassword(password: String): String {
    val BCRYPT_SALT = "\$2a\$10\$QCJoWqnN.acrjPIgKYCthu"
    return BCrypt.hashpw(password, BCRYPT_SALT)
}