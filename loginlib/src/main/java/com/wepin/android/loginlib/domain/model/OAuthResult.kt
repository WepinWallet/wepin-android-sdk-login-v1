package com.wepin.android.loginlib.domain.model

sealed class OAuthResult {
    data class Success(
        val provider: String,
        val token: String,
        val tokenType: TokenType
    ) : OAuthResult()
    
    data class Error(
        val code: String? = null,
        val message: String? = null
    ) : OAuthResult()
    
    object Loading : OAuthResult()
}

enum class TokenType {
    ID_TOKEN,
    ACCESS_TOKEN
} 