plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wepin.loginlibrary"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wepin.loginlibrary"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // For Deep Link => RedirectScheme Format : wepin. + Wepin App ID
        manifestPlaceholders["appAuthRedirectScheme"] =  "wepin.appid"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // WepinLoginLib With project link
     implementation(project(":loginlib"))
    //implementation("com.github.WepinWallet:wepin-android-sdk-login-v1:v0.0.1")


    // WepinLoginLib With aar
//    implementation(files("libs/wepin-login-v0.0.1.aar"))
//    implementation ("org.bitcoinj:bitcoinj-core:0.15.10")
//    implementation ("com.google.code.gson:gson:2.9.1")
//    implementation ("net.openid:appauth:0.11.1")
//    implementation ("androidx.security:security-crypto-ktx:1.1.0-alpha03")
//    implementation ("org.mindrot:jbcrypt:0.4")
//    implementation ("com.android.volley:volley:1.2.1")



    // Google Login
    implementation ("com.google.android.gms:play-services-auth:19.2.0")

    // Naver Login
//    implementation("com.navercorp.nid:oauth:5.9.1") // jdk 11

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}