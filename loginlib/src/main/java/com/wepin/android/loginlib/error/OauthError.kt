package com.wepin.android.loginlib.error

import net.openid.appauth.AuthorizationException

fun getOauthErrorCode(error: AuthorizationException, defaultCode: String): String {
    return when (error.type) {
        AuthorizationException.TYPE_GENERAL_ERROR -> {
            when (error.code) {
                AuthorizationException.GeneralErrors.NETWORK_ERROR.code -> "network_error"
                AuthorizationException.GeneralErrors.ID_TOKEN_PARSING_ERROR.code -> "id_token_parsing_error"
                AuthorizationException.GeneralErrors.JSON_DESERIALIZATION_ERROR.code -> "json_deserialization_error"
                AuthorizationException.GeneralErrors.SERVER_ERROR.code -> "server_error"
                AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR.code -> "id_token_validation_error"
                AuthorizationException.GeneralErrors.INVALID_DISCOVERY_DOCUMENT.code -> "invalid_discovery_document"
                AuthorizationException.GeneralErrors.INVALID_REGISTRATION_RESPONSE.code -> "invalid_registration_response"
                AuthorizationException.GeneralErrors.PROGRAM_CANCELED_AUTH_FLOW.code -> "user_canceled"
                AuthorizationException.GeneralErrors.INVALID_REGISTRATION_RESPONSE.code -> "invalid_registration_response"
                AuthorizationException.GeneralErrors.TOKEN_RESPONSE_CONSTRUCTION_ERROR.code -> "token_response_construction_error"
                AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW.code -> "user_canceled"
                else -> defaultCode
            }
        }

        AuthorizationException.TYPE_OAUTH_AUTHORIZATION_ERROR -> {
            when (error.code) {
                AuthorizationException.AuthorizationRequestErrors.SERVER_ERROR.code -> "server_error"
                AuthorizationException.AuthorizationRequestErrors.CLIENT_ERROR.code -> "client_error"
                AuthorizationException.AuthorizationRequestErrors.INVALID_REQUEST.code -> "invalid_request"
                AuthorizationException.AuthorizationRequestErrors.ACCESS_DENIED.code -> "access_denied"
                AuthorizationException.AuthorizationRequestErrors.INVALID_SCOPE.code -> "invalid_scope"
                AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH.code -> "state_mismatch"
                AuthorizationException.AuthorizationRequestErrors.TEMPORARILY_UNAVAILABLE.code -> "temporarily_unavailable"
                AuthorizationException.AuthorizationRequestErrors.UNAUTHORIZED_CLIENT.code -> "unauthorized_client"
                AuthorizationException.AuthorizationRequestErrors.UNSUPPORTED_RESPONSE_TYPE.code -> "unsupported_response_type"
                else -> defaultCode
            }
        }

        AuthorizationException.TYPE_OAUTH_TOKEN_ERROR -> {
            when (error.code) {
                AuthorizationException.TokenRequestErrors.CLIENT_ERROR.code -> "client_error"
                AuthorizationException.TokenRequestErrors.INVALID_REQUEST.code -> "invalid_request"
                AuthorizationException.TokenRequestErrors.INVALID_SCOPE.code -> "invalid_scope"
                AuthorizationException.TokenRequestErrors.UNAUTHORIZED_CLIENT.code -> "unauthorized_client"
                AuthorizationException.TokenRequestErrors.INVALID_CLIENT.code -> "invalid_client"
                AuthorizationException.TokenRequestErrors.INVALID_GRANT.code -> "invalid_grant"
                AuthorizationException.TokenRequestErrors.UNSUPPORTED_GRANT_TYPE.code -> "unsupported_grant_type"
                else -> defaultCode
            }
        }

        AuthorizationException.TYPE_OAUTH_REGISTRATION_ERROR -> {
            when (error.code) {
                AuthorizationException.RegistrationRequestErrors.CLIENT_ERROR.code -> "client_error"
                AuthorizationException.RegistrationRequestErrors.INVALID_REQUEST.code -> "invalid_request"
                AuthorizationException.RegistrationRequestErrors.INVALID_REDIRECT_URI.code -> "invalid_redirect_url"
                AuthorizationException.RegistrationRequestErrors.INVALID_CLIENT_METADATA.code -> "invalid_client_metadata"
                else -> defaultCode
            }
        }

        AuthorizationException.TYPE_RESOURCE_SERVER_AUTHORIZATION_ERROR -> "resource_server_authorization_error"
        else -> defaultCode
    }
}

fun getOauthErrorMessage(exception: AuthorizationException): String {
    return exception.localizedMessage ?: "Unknown error"
}
