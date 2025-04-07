package com.wepin.android.loginlib

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.wepin.android.loginlib.R

class RedirectUriReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 배경을 투명하게 설정
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(R.layout.activity_wepin_login_main)
//        setContentView(R.layout.activity_redirect_uri_recever)
        // 외부 모듈의 액티비티 호출
        val intent: Intent = Intent(
            this@RedirectUriReceiverActivity,
            net.openid.appauth.RedirectUriReceiverActivity::class.java
        )
        startActivity(intent)
    }
}