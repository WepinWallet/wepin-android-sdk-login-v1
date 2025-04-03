package com.wepin.android.loginlib.domain.repository

import com.wepin.android.loginlib.domain.model.OAuthResult
import kotlinx.coroutines.flow.Flow

interface ILoginRepository {
    fun getOAuthToken(
        provider: String,
        code: String,
        clientId: String,
        state: String?,
        codeVerifier: String?
    ): Flow<OAuthResult>
    
    fun getRedirectUrl(): String
} 