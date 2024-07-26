package com.wepin.android.loginlib.types.network.firebase

internal data class EmailAndPasswordRequest (
    var email:String,
    var password:String,
    var returnSecureToken: Boolean = true) {

}