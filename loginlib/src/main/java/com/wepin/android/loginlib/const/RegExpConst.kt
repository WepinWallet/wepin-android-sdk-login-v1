package com.wepin.android.loginlib.const

internal object RegExpConst {
    private val emailRegExp = Regex("[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", RegexOption.IGNORE_CASE)
    private val passwordRegExp = Regex("^(?=.*[a-zA-Z])(?=.*[0-9]).{8,128}\$")
    private val pinRegExp = Regex("^\\d{6,8}\$")

    fun validateEmail(email: String?): Boolean{
        email?.let {
            return emailRegExp.matches(it)
        }
        return false
    }

    fun validatePassword(password: String?): Boolean {
        password?.let {
            return passwordRegExp.matches(it)
        }
        return false
    }

    fun validatePin(pin: String?): Boolean {
        pin?.let {
            return pinRegExp.matches(it)
        }
        return false
    }
}