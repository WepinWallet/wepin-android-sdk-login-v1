package com.wepin.android.loginlib.manager

import com.wepin.android.commonlib.types.WepinLoginStatus
import com.wepin.android.commonlib.types.WepinToken
import com.wepin.android.commonlib.types.WepinUser
import com.wepin.android.commonlib.types.WepinUserInfo
import com.wepin.android.commonlib.types.WepinUserStatus
import com.wepin.android.core.storage.WepinStorageManager
import com.wepin.android.core.types.storage.StorageDataType
import com.wepin.android.core.types.storage.UserInfoDetails
import com.wepin.android.core.types.wepin.LoginResponse
import com.wepin.android.core.utils.Log
import com.wepin.android.loginlib.types.LoginResult
import com.wepin.android.loginlib.types.Providers

object WepinLoginStorageManager {

    fun setWepinUser(request: LoginResult, response: LoginResponse) {
        WepinStorageManager.deleteAllStorage()
        WepinStorageManager.setStorage(
            "firebase:wepin",
            StorageDataType.FirebaseWepin(
                idToken = request.token.idToken,
                refreshToken = request.token.refreshToken,
                provider = request.provider.value
            )
        )
        WepinStorageManager.setStorage(
            "wepin:connectUser",
            StorageDataType.WepinToken(
                accessToken = response.token.access,
                refreshToken = response.token.refresh
            )
        )
        WepinStorageManager.setStorage("user_id", response.userInfo.userId)

        WepinStorageManager.setStorage(
            "user_status",
            StorageDataType.UserStatus(
                loginStatus = response.loginStatus,
                pinRequired = (if (response.loginStatus == "registerRequired") response.pinRequired else false)
            )
        )

        if (response.loginStatus != "pinRequired" && response.walletId != null) {
            WepinStorageManager.setStorage("wallet_id", response.walletId)
            WepinStorageManager.setStorage<StorageDataType>(
                "user_info",
                StorageDataType.UserInfo(
                    status = "success",
                    userInfo = UserInfoDetails(
                        userId = response.userInfo.userId,
                        email = response.userInfo.email,
                        provider = request.provider.value,
                        use2FA = (response.userInfo.use2FA >= 2),
                    ),
                    walletId = response.walletId
                )
            )
        } else {
            val userInfo = StorageDataType.UserInfo(
                status = "success",
                userInfo = UserInfoDetails(
                    userId = response.userInfo.userId,
                    email = response.userInfo.email,
                    provider = request.provider.value,
                    use2FA = (response.userInfo.use2FA >= 2),
                ),
            )
            WepinStorageManager.setStorage<StorageDataType>("user_info", userInfo)
        }
        WepinStorageManager.setStorage("oauth_provider_pending", request.provider.value)
    }

    fun setFirebaseUser(loginResult: LoginResult) {
        WepinStorageManager.deleteAllStorage()
        WepinStorageManager.setStorage(
            "firebase:wepin",
            StorageDataType.FirebaseWepin(
                idToken = loginResult.token.idToken,
                refreshToken = loginResult.token.refreshToken,
                provider = loginResult.provider.value
            )
        )
    }

    fun getWepinUser(): WepinUser? {
        try {
            val walletId = WepinStorageManager.getStorage<String>("wallet_id")
            val userInfo = WepinStorageManager.getStorage<StorageDataType>("user_info")
            val wepinToken = WepinStorageManager.getStorage<StorageDataType>("wepin:connectUser")
            val userStatus = WepinStorageManager.getStorage<StorageDataType>("user_status")

            if (userInfo == null || wepinToken == null || userStatus == null) {
                return null
            }

            val wepinUser = WepinUser(
                status = "success",
                userInfo = WepinUserInfo(
                    userId = (userInfo as StorageDataType.UserInfo).userInfo!!.userId,
                    email = userInfo.userInfo!!.email,
                    provider = Providers.fromValue(userInfo.userInfo!!.provider)!!,
                    use2FA = userInfo.userInfo!!.use2FA
                ),
                userStatus = WepinUserStatus(
//                    loginStatus = WepinLoginStatus.fromValue((userStatus as StorageDataType.UserStatus).loginStatus)!!,
                    loginStatus = WepinLoginStatus.fromValue((userStatus as StorageDataType.UserStatus).loginStatus),
                    pinRequired = userStatus.pinRequired
                ),
                walletId = walletId,
                token = WepinToken(
                    accessToken = (wepinToken as StorageDataType.WepinToken).accessToken,
                    refreshToken = wepinToken.refreshToken
                )
            )
            return wepinUser
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}