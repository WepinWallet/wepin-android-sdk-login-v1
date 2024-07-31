<br/>

<p align="center">
  <a href="https://www.wepin.io/">
      <picture>
        <source media="(prefers-color-scheme: dark)">
        <img alt="wepin logo" src="https://github.com/WepinWallet/wepin-web-sdk-v1/blob/main/assets/wepin_logo_color.png?raw=true" width="250" height="auto">
      </picture>
</a>
</p>

<br>


# Wepin Android SDK Login Library v1

[![platform - android](https://img.shields.io/badge/platform-Android-3ddc84.svg?logo=android&style=for-the-badge)](https://www.android.com/)

Wepin Login Library for Android. This package is exclusively available for use in Android environments.

## ⏩ Get App ID and Key
After signing up for [Wepin Workspace](https://workspace.wepin.io/), go to the development tools menu and enter the information for each app platform to receive your App ID and App Key.


## ⏩ Requirements
- Android API version 24 or newer is required.

## ⏩ Getting Started

1. Add JitPack repository in your project-level build gradle file
 - kts
   ```kotlin
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            google()
            mavenCentral()
            maven("https://jitpack.io") // <= Add JitPack Repository
        }
    }
   ```
2. Add implementation in your app-level build gradle file
  - kts
    ```
    dependencies {
      // ...
      implementation("com.github.WepinWallet:wepin-android-sdk-login-v1:vX.X.X") 
    }
    ```
    > **<span style="font-size: 35px;"> !!Caution!! </span>**  We recommend using [the latest released version of the SDK](https://github.com/WepinWallet/wepin-android-sdk-login-v1/releases)

## ⏩ Add Permission
Add the below line in your app's `AndroidManifest.xml` file

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />

```

## ⏩ Configure Deep Link
Deep Link scheme format : `wepin. + Your Wepin App ID`

When a custom scheme is used, WepinLogin Library can be easily configured to capture all redirects using this custom scheme through a manifest placeholder:

```kotlin
// For Deep Link => RedirectScheme Format : wepin. + Wepin App ID
android.defaultConfig.manifestPlaceholders = [
  'appAuthRedirectScheme': 'wepin.{{YOUR_WEPIN_APPID}}'
]
```
Add the below line in your app's AndroidManifest.xml file

```xml
<activity
  android:name="com.wepin.android.loginlib.RedirectUriReceiverActivity"
  android:exported="true">
  <intent-filter>
     <action android:name="android.intent.action.VIEW" />

     <category android:name="android.intent.category.DEFAULT" />
     <category android:name="android.intent.category.BROWSABLE" />
     <data
      android:host="oauth2redirect"
      android:scheme="${appAuthRedirectScheme}" />
  </intent-filter>
</activity>
```

## ⏩ Import
```kotlin
  import com.wepin.android.loginlib.WepinLogin;
```

## ⏩ Initialization
Create instance of WepinLoginLibrary in your activity to use wepin and pass your activity as a parameter

- java
  ```java
  public class MainActivity extends ComponentActivity {
    private WepinLogin wepinLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example_main);

        String appId = getResources().getString(R.string.wepin_app_id);
        String appKey = getResources().getString(R.string.wepin_app_key);

        WepinLoginOptions wepinLoginOptions = new WepinLoginOptions(this, appId, appKey);
        wepinLogin = new WepinLogin(wepinLoginOptions);

        // Call initialize function
        CompletableFuture<Boolean> res = wepinLogin.init();
        res.whenComplete((infResponse, error) -> {
            if (error == null) {
                System.out.println("infResponse: " + infResponse);
            } else {
                // render error UI
            }
        });
    }
  }
  ```
- kotlin
  ```kotlin
  class MainActivity : ComponentActivity() {
      private lateinit var wepinLogin: WepinLogin
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)

          setContentView(R.layout.activity_example_main)

          val wepinLoginOptions =  WepinLoginOptions(
              context = this,
              appId=resources.getString(R.string.wepin_app_id),
              appKey = resources.getString(R.string.wepin_app_key)
          )
          wepinLogin = WepinLogin(wepinLoginOptions)
          // Call initialize function 
          val res: CompletableFuture<Void> = wepinLogin.init()
          res.whenComplete { infResponse, error ->
              if (error == null) {
                  println("infResponse: $infResponse")
              } else {
                  // render error UI
              }
      }
      // ...
  }
  ```

### isInitialized
```kotlin
wepinLogin.isInitialized()
```
The `isInitialized()` method checks Wepin Login Library is initialized.

#### Returns
- \<Boolean>
    - true if Wepin Login Library is already initialized.

## ⏩ Method
Methods can be used after initialization of Wepin Login Library.

### loginWithOauthProvider
```kotlin
wepinLogin.loginWithOauthProvider(params)
```

An in-app browser will open and proceed to log in to the OAuth provider. To retrieve Firebase login information, you need to execute either the loginWithIdToken() or loginWithAccessToken() method. 

#### Parameters
- `params` \<LoginOauth2Params> 
  - `provider` \<'google'|'naver'|'discord'|'apple'> - Provider for login
  - `clientId` \<String>
  
#### Returns
- CompletableFuture\<LoginOauthResult>
  - `provider` \<String> - login provider
  - `token` \<String> - accessToken (if provider is "naver" or "discord") or idToken (if provider is "google" or "apple")
  - `type` \<OauthTokenType> - type of token

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- java
  ```java
  LoginOauth2Params loginOption = new LoginOauth2Params(
                      provider = "discord",
                      clientId = getString(R.string.default_discord_client_id),
                    )
  CompletableFuture<LoginOauthResult> res = wepinLogin.loginWithOauthProvider(loginOption);
  res.whenComplete((loginResponse, error) -> {
      if (error == null) {
          System.out.println("loginResponse: " + loginResponse);
          String privateKey = "private key for wepin id/access Token"
          // token sign 
          String sign = wepinLogin.getSignForLogin(loginResponse.token, privateKey)
          //call loginWithIdToken() or loginWithAccessToken()
      } else {
          // render error UI
          System.out.println("login error" + error.getMessage())
      }
  });
  ```
- kotlin
  ```kotlin
    val loginOption = LoginOauth2Params(
                        provider = "discord",
                        clientId = getString(R.string.default_discord_client_id),
                      )
    wepinLogin.loginWithOauthProvider(loginOption).whenComplete { loginResponse, error ->
        if (error == null) {
            // render logged in UI
            println(loginResponse)
            val privateKey = "private key for wepin id/access Token"
            // token sign 
            wepinLogin.getSignForLogin(loginResponse.token, privateKey)
            //call loginWithIdToken() or loginWithAccessToken()
        } else {
            println("login error - ${error.message}")
            // render error UI
        }
    }

  ```

### signUpWithEmailAndPassword
```kotlin
wepinLogin.signUpWithEmailAndPassword(params)
```

This function signs up on Wepin Firebase with your email and password. It returns Firebase login information upon successful signup.

#### Parameters
- `params` \<LoginWithEmailParams> 
  - `email` \<String> - User email
  - `password` \<String> -  User password
  - `locale` \<String> - __optional__ Language for the verification email (default value: "en")

#### Returns
- CompletableFuture\<LoginResult>
  - `provider` \<Providers.EMAIL>
  - `token` \<FBToken>
    - `idToken` \<String> - wepin firebase idToken
    - `refreshToken` `<String> - wepin firebase refreshToken

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- java
  ```java
  LoginWithEmailParams loginOption = new LoginWithEmailParams(
                      "abc@defg.com",
                      "abcdef123&",
                      "ko"
                    )
  CompletableFuture<LoginResult> res = wepinLogin.signUpWithEmailAndPassword(loginOption);
  res.whenComplete((loginResponse, error) -> {
      if (error == null) {
          System.out.println("loginResponse: " + infResponse);
      } else {
          // render error UI
          System.out.println("login error" + error.getMessage())
      }
  });
  ```
- kotlin
  ```kotlin
    val loginOption = LoginWithEmailParams(
                        email = "abc@defg.com",
                        password = "abcdef123&",
                        language = "ko"
                      )
    wepinLogin.signUpWithEmailAndPassword(loginOption).whenComplete { loginResponse, error ->
        if (error == null) {
            // render logged in UI
            println(loginResponse)
            
        } else {
            println("login error - ${error.message}")
            // render error UI
        }
    }

  ```

### loginWithEmailAndPassword
```java
wepinLogin.loginWithEmailAndPassword(params)
```

This function logs in to the Wepin Firebase using your email and password. It returns Firebase login information upon successful login.

#### Parameters
- `params` \<LoginWithEmailParams> 
  - `email` \<String> - User email
  - `password` \<String> -  User password

#### Returns
- CompletableFuture\<LoginResult>
  - `provider` \<Providers.EMAIL>
  - `token` \<FBToken>
    - `idToken` \<String> - wepin firebase idToken
    - `refreshToken` `<String> - wepin firebase refreshToken

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- java
  ```java
  LoginWithEmailParams loginOption = new LoginWithEmailParams(
                      "abc@defg.com",
                      "abcdef123&",
                    )
  CompletableFuture<LoginResult> res = wepinLogin.loginWithEmailAndPassword(loginOption);
  res.whenComplete((loginResponse, error) -> {
      if (error == null) {
          System.out.println("loginResponse: " + infResponse);
      } else {
          // render error UI
          System.out.println("login error" + error.getMessage())
      }
  });
  ```
- kotlin
  ```kotlin
    val loginOption = LoginWithEmailParams(
                        email = "abc@defg.com",
                        password = "abcdef123&",
                        language = "ko"
                      )
    wepinLogin.loginWithEmailAndPassword(loginOption).whenComplete { loginResponse, error ->
        if (error == null) {
            // render logged in UI
            println(loginResponse)
            
        } else {
            println("login error - ${error.message}")
            // render error UI
        }
    }

  ```

### loginWithIdToken
```java
wepinLogin.loginWithIdToken(params)
```

This function logs in to the Wepin Firebase using an external ID token. It returns Firebase login information upon successful login.

#### Parameters
- `params` \<LoginOauthIdTokenRequest> 
  - `token` \<String> - ID token value to be used for login
  - `sign` \<String> - Signature value for the token provided as the first parameter.(Returned value of [getSignForLogin()](#getSignForLogin))


#### Returns
- CompletableFuture\<LoginResult>
  - `provider` \<Providers.EXTERNAL_TOKEN>
  - `token` \<FBToken>
    - `idToken` \<String> - wepin firebase idToken
    - `refreshToken` `<String> - wepin firebase refreshToken

#### Exception
- [Wepin Error](#wepin-error)

#### Example
- java
  ```java
  String token = "eyJHGciO....adQssw5c"
  String sign = "9753d4dc...c63466b9"
  LoginWithEmailParams loginOption = new LoginOauthIdTokenRequest(token, sign)
  CompletableFuture<LoginResult> res = wepinLogin.loginWithIdToken(loginOption);
  res.whenComplete((loginResponse, error) -> {
      if (error == null) {
          System.out.println("loginResponse: " + infResponse);
      } else {
          // render error UI
          System.out.println("login error" + error.getMessage())
      }
  });
  ```
- kotlin
  ```kotlin
    val token = "eyJHGciO....adQssw5c"
    val sign = "9753d4dc...c63466b9"
    val loginOption = new LoginOauthIdTokenRequest(token, sign)
    wepinLogin.loginWithIdToken(loginOption).whenComplete { loginResponse, error ->
        if (error == null) {
            // render logged in UI
            println(loginResponse)
            
        } else {
            println("login error - ${error.message}")
            // render error UI
        }
    }

  ```

### loginWithAccessToken
```java
wepinLogin.loginWithAccessToken(params)
```

This function logs in to the Wepin Firebase using an external access token. It returns Firebase login information upon successful login.

#### Parameters
- `params` \<LoginOauthAccessTokenRequest> 
  - `provider` \<"naver"|"discord"> - Provider that issued the access token
  - `accessToken` \<String> - Access token value to be used for login 
  - `sign` \<String> - Signature value for the token provided as the first parameter. (Returned value of [getSignForLogin()](#getSignForLogin))


#### Returns
- CompletableFuture\<LoginResult>
  - `provider` \<Providers.EXTERNAL_TOKEN>
  - `token` \<FBToken>
    - `idToken` \<String> - wepin firebase idToken
    - `refreshToken` `<String> - wepin firebase refreshToken


#### Exception
- [Wepin Error](#wepin-error)

#### Example
- java
  ```java
  String accessToken = "eyJHGciO....adQssw5c"
  String sign = "9753d4dc...c63466b9"
  LoginOauthAccessTokenRequest loginOption = new LoginOauthAccessTokenRequest("discord", accessToken, sign)
  CompletableFuture<LoginResult> res = wepinLogin.loginWithAccessToken(loginOption);
  res.whenComplete((loginResponse, error) -> {
      if (error == null) {
          System.out.println("loginResponse: " + infResponse);
      } else {
          // render error UI
          System.out.println("login error" + error.getMessage())
      }
  });
  ```
- kotlin
  ```kotlin
    val accessToken = "eyJHGciO....adQssw5c"
    val sign = "9753d4dc...c63466b9"
    val loginOption = LoginOauthAccessTokenRequest(
            provider = "discord",
            accessToken = accessToken, sign = sign
        )
    wepinLogin.loginWithAccessToken(loginOption).whenComplete { loginResponse, error ->
        if (error == null) {
            // render logged in UI
            println(loginResponse)
            
        } else {
            println("login error - ${error.message}")
            // render error UI
        }
    }

  ```

### getRefreshFirebaseToken
```java
wepinLogin.getRefreshFirebaseToken()
```

This method retrieves the current firebase token's information from the Wepin.

#### Parameters
- void

#### Returns
- CompletableFuture\<LoginResult>
  - `provider` \<Providers>
  - `token` \<FBToken>
    - `idToken` \<String> - wepin firebase idToken
    - `refreshToken` `<String> - wepin firebase refreshToken

#### Exception
- [Wepin Error](#wepin-error)

#### Example

- java
  ```java
  CompletableFuture<WepinUser> res = wepinLogin.getRefreshFirebaseToken();
  res.whenComplete((firebaseResponse, error) -> {
    if (error == null) {
        System.out.println("firebaseResponse: " + firebaseResponse);
        // render logged in UI
          println(firebaseResponse)
    } else {
        // render error UI
        System.out.println("login error: " + error.getMessage());
    }
  });
  ```
- kotlin
  ```kotlin
    wepinLogin.getRefreshFirebaseToken().whenComplete { firebaseResponse, error ->
    if (error == null) {
        println(firebaseResponse)
        // render logged in UI
    } else {
        println("login error - ${error.message}")
        // render error UI
    }
  }
  ```


### loginWepin
```java
wepinLogin.loginWepin(param)
```

This method logs the user into the Wepin application using the specified provider and token.

#### Parameters
The parameters should utilize the return values from the `loginWithEmailAndPassword()`, `loginWithIdToken()`, and `loginWithAccessToken()` methods within this module.
- \<LoginResult>
  - provider \<Providers>
  - token \<FBToken>
    - idToken \<String> - Wepin Firebase idToken
    - refreshToken\<String> - Wepin Firebase refreshToken

#### Returns
- CompletableFuture\<WepinUser> - A promise that resolves to an object containing the user's login status and information. The object includes:
  - status \<'success'|'fail'>  - The login status.
  - userInfo \<UserInfo> __optional__ - The user's information, including:
    - userId \<String> - The user's ID.
    - email \<String> - The user's email.
    - provider \<'google'|'apple'|'naver'|'discord'|'email'|'external_token'> - The login provider.
    - use2FA \<Boolean> - Whether the user uses two-factor authentication.
  - walletId \<String> = The user's wallet ID.
  - userStatus: \<UserStatus> - The user's status of wepin login. including:
    - loginStats: \<'complete' | 'pinRequired' | 'registerRequired'> - If the user's loginStatus value is not complete, it must be registered in the wepin.
    - pinRequired?: <Boolean> 
  - token: \<Token> - The user's token of wepin.
    - accessToken: \<String>
    - refreshToken \<String>

#### Exception
- [Wepin Error](#wepin-error)

#### Example

- java
  ```java
  String accessToken = "eyJHGciO....adQssw5c"
  String sign = "9753d4dc...c63466b9"
  LoginOauthAccessTokenRequest loginOption = new LoginOauthAccessTokenRequest("discord", accessToken, sign)
  CompletableFuture<LoginResult> res = wepinLogin.loginWithAccessToken(loginOption);
  res.whenComplete((loginResponse, error) -> {
    if (error == null) {
        System.out.println("loginResponse: " + loginResponse);
        CompletableFuture<LoginResult> resWepin = wepinLogin.loginWepin(loginResponse);
        resWepin.whenComplete((loginWepinResponse, error) -> {
          if (error == null) {
              System.out.println("loginWepinResponse: " + loginWepinResponse);
              if(loginWepinResponse.loginStatus === WepinLoginStatus.PIN_REQUIRED||loginWepinResponse.loginStatus === WepinLoginStatus.REGISTER_REQUIRED) {
                  // wepin registration
              }else {
                // render logged in UI
              }                
          } else {
              // render error UI
              System.out.println("login error" + error.getMessage())
          }
        })
    } else {
        // render error UI
        System.out.println("login error" + error.getMessage())
  }
  });
  ```
- kotlin
  ```kotlin
    val accessToken = "eyJHGciO....adQssw5c"
    val sign = "9753d4dc...c63466b9"
    val loginOption = LoginOauthAccessTokenRequest(
            provider = "discord",
            accessToken = accessToken, sign = sign
        )
    wepinLogin.loginWithAccessToken(loginOption).whenComplete { loginResponse, error ->
        if (error == null) {
            println(loginResponse)
            wepinLogin.loginWepin(loginResponse).whenComplete { loginWepinResponse, err ->
              if (err == null) {
                  
                  println(loginWepinResponse)
                  if(loginWepinResponse.loginStatus === WepinLoginStatus.PIN_REQUIRED||loginWepinResponse.loginStatus === WepinLoginStatus.REGISTER_REQUIRED) {
                    // wepin registration
                }else {
                  // render logged in UI
                }                 
              } else {
                  println("login err - ${err.message}")
                  // render error UI
              }
          }   
        } else {
            println("login error - ${error.message}")
            // render error UI
        }
    }

  ```

### getCurrentWepinUser
```java
wepinLogin.getCurrentWepinUser()
```

This method retrieves the current logged-in user's information from the Wepin.

#### Parameters
- void

#### Returns
- CompletableFuture\<WepinUser> - A promise that resolves to an object containing the user's login status and information. The object includes:
  - status \<'success'|'fail'>  - The login status.
  - userInfo \<UserInfo> __optional__ - The user's information, including:
    - userId \<String> - The user's ID.
    - email \<String> - The user's email.
    - provider \<'google'|'apple'|'naver'|'discord'|'email'|'external_token'> - The login provider.
    - use2FA \<Boolean> - Whether the user uses two-factor authentication.
  - walletId \<String> = The user's wallet ID.
  - userStatus: \<UserStatus> - The user's status of wepin login. including:
    - loginStats: \<'complete' | 'pinRequired' | 'registerRequired'> - If the user's loginStatus value is not complete, it must be registered in the wepin.
    - pinRequired?: <Boolean> 
  - token: \<Token> - The user's token of wepin.
    - accessToken: \<String>
    - refreshToken \<String>

#### Exception
- [Wepin Error](#wepin-error)

#### Example

- java
  ```java
  CompletableFuture<WepinUser> res = wepinLogin.getCurrentWepinUser();
  res.whenComplete((wepinUserResponse, error) -> {
    if (error == null) {
        System.out.println("wepinUserResponse: " + wepinUserResponse);
        if (wepinUserResponse.loginStatus == WepinLoginStatus.PIN_REQUIRED || wepinUserResponse.loginStatus == WepinLoginStatus.REGISTER_REQUIRED) {
            // wepin registration
        } else {
            // render logged in UI
        }
    } else {
        // render error UI
        System.out.println("login error: " + error.getMessage());
    }
  });
  ```
- kotlin
  ```kotlin
    wepinLogin.getCurrentWepinUser().whenComplete { wepinUserResponse, error ->
    if (error == null) {
        println(wepinUserResponse)
        if (wepinUserResponse.loginStatus == WepinLoginStatus.PIN_REQUIRED || wepinUserResponse.loginStatus == WepinLoginStatus.REGISTER_REQUIRED) {
            // wepin registration
        } else {
            // render logged in UI
        }
    } else {
        println("login error - ${error.message}")
        // render error UI
    }
  }
  ```


### logoutWepin
```java
wepinLogin.logoutWepin()
```

The `logoutWepin()` method logs out the user logged into Wepin.

#### Parameters
 - void
#### Returns
- CompletableFuture\<Boolean>
  
#### Exception
- [Wepin Error](#wepin-error)

#### Example
- java
  ```java
  CompletableFuture<Boolean> res = wepinLogin.logoutWepin();
  res.whenComplete((logoutResponse, error) -> {
    if (error == null) {
        System.out.println("logoutResponse: " + logoutResponse);
    } else {
        // render error UI
        System.out.println("logout error" + error.getMessage())
    }
  });
  ```

- kotlin
  ```kotlin
  wepinLogin.logoutWepin().whenComplete { logoutResponse, error ->
    if (error == null) {
        println(logoutResponse)
    } else {
        println("logout error - ${error.message}")
        // render error UI
    }
  }
  ```

### getSignForLogin
Generates signatures to verify the issuer. It is mainly used to generate signatures for login-related information such as ID tokens and access tokens.

```java
wepinLogin.getSignForLogin(privKey, message);
```

#### Parameters
- `privKey` \<String> - The authentication key used for signature generation.
- `message` \<String> - The message or payload to be signed.

#### Returns
- String - The generated signature.

> ‼️ Caution ‼️
> 
> The authentication key (`privKey`) must be stored securely and must not be exposed to the outside. It is recommended to execute the `getSignForLogin()` method on the backend rather than the frontend for enhanced security and protection of sensitive information.

#### Example
- java
  ```java
  String privKey = '0400112233445566778899001122334455667788990011223344556677889900'
  String idToken = 'idtokenabcdef'
  String sign = wepinLogin.getSignForLogin(privKey, idToken)
  ```
- kotlin
  ```kotlin
  val privKey = '0400112233445566778899001122334455667788990011223344556677889900'
  val idToken = 'idtokenabcdef'
  val sign = wepinLogin.getSignForLogin(privKey, idToken)
  ```


### finalize
```java
wepinLogin.finalize()
```

The `finalize()` method finalizes the Wepin Login Library.

#### Parameters
 - void
#### Returns
 - void

#### Example
- java
  ```java
  wepinLogin.finalize()
  ```
- kotlin
  ```kotlin
  wepinLogin.finalize()
  ```

### Wepin Error
| Error Code                   | Error Message                      | Error Description                                                                                   |
|------------------------------|------------------------------------|-----------------------------------------------------------------------------------------------------|
| `INVALID_APP_KEY`            | "Invalid app key"                  | The Wepin app key is invalid.                                                                       |
| `INVALID_PARAMETER` `        | "Invalid parameter"                | One or more parameters provided are invalid or missing.                                             |
| `INVALID_LOGIN_PROVIDER`     | "Invalid login provider"           | The login provider specified is not supported or is invalid.                                        |
| `INVALID_TOKEN`              | "Token does not exist"             | The token does not exist.                                                                           |
| `INVALID_LOGIN_SESSION`      | "Invalid Login Session"            | The login session information does not exist.                                                                           |
| `NOT_INITIALIZED_ERROR`      | "Not initialized error"            | The WepinLoginLibrary has not been properly initialized.                                            |
| `ALREADY_INITIALIZED_ERROR`  | "Already initialized"              | The WepinLoginLibrary is already initialized, so the logout operation cannot be performed again.    |
| `NOT_ACTIVITY`               | "Context is not activity"          | The Context is not an activity                                                                      |
| `USER_CANCELLED`             | "User cancelled"                   | The user has cancelled the operation.                                                               |
| `UNKNOWN_ERROR`              | "An unknown error occurred"        | An unknown error has occurred, and the cause is not identified.                                     |
| `NOT_CONNECTED_INTERNET`     | "No internet connection"           | The system is unable to detect an active internet connection.                                       |
| `FAILED_LOGIN`               | "Failed to Oauth log in"           | The login attempt has failed due to incorrect credentials or other issues.                          |
| `ALREADY_LOGOUT`             | "Already Logout"                   | The user is already logged out, so the logout operation cannot be performed again.                  |
| `INVALID_EMAIL_DOMAIN`       | "Invalid email domain"             | The provided email address's domain is not allowed or recognized by the system.                     |
| `FAILED_SEND_EMAIL`          | "Failed to send email"             | The system encountered an error while sending an email. This is because the email address is invalid or we sent verification emails too often. Please change your email or try again after 1 minute.                   |
| `REQUIRED_EMAIL_VERIFIED`    | "Email verification required"      | Email verification is required to proceed with the requested operation.                             |
| `INCORRECT_EMAIL_FORM`       | "Incorrect email format"           | The provided email address does not match the expected format.                                      |
| `INCORRECT_PASSWORD_FORM`    | "Incorrect password format"        | The provided password does not meet the required format or criteria.                                |
| `NOT_INITIALIZED_NETWORK`    | "Network Manager not initialized"  | The network or connection required for the operation has not been properly initialized.             |
| `REQUIRED_SIGNUP_EMAIL`      | "Email sign-up required."          | The user needs to sign up with an email address to proceed.                                         |
| `FAILED_EMAIL_VERIFIED`      | "Failed to verify email."          | The WepinLoginLibrary encountered an issue while attempting to verify the provided email address.   |
| `FAILED_PASSWORD_SETTING`    | "Failed to set password."          | The WepinLoginLibrary failed to set the password.                                                   |
| `EXISTED_EMAIL`              | "Email already exists."            | The provided email address is already registered in Wepin.                                          |
