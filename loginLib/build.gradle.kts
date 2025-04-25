plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

val sdkVersion = project.findProperty("wepinAndroidSdkVersion") ?: "LOCAL-SNAPSHOT"
rootProject.extra["wepinAndroidSdkVersion"] = sdkVersion

android {
    namespace = "com.wepin.android.loginlib"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        buildConfigField(
            "String",
            "LIBRARY_VERSION",
            "\"${rootProject.extra["wepinAndroidSdkVersion"]}\""
        )
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true // buildConfig 기능을 활성화합니다.
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
    libraryVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = when (name) {
                "release" -> "wepin-login-v${project.extra["wepinAndroidSdkVersion"]}.aar"
                "debug" -> "debug-wepin-login-v${project.extra["wepinAndroidSdkVersion"]}.aar"
                else -> throw IllegalArgumentException("Unsupported build variant: $name")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Wepin
//    api(project(":libs:common:commonLib"))
//    implementation(project(":libs:core:coreLib"))
    api("io.wepin:wepin-android-sdk-common-v1:${sdkVersion}")
    implementation("io.wepin:wepin-android-sdk-core-v1:${sdkVersion}")

    // ECDSA
//    implementation("org.bitcoinj:bitcoinj-core:0.15.10")

    // AppAuth
    implementation("net.openid:appauth:0.11.1")

    // becrypt
    implementation("org.mindrot:jbcrypt:0.4")

    // Activity KTX for viewModels()
    implementation("androidx.activity:activity-ktx:1.8.2")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.WepinWallet"
                artifactId = "wepin-android-sdk-login-v1"
            }
        }
    }
}

