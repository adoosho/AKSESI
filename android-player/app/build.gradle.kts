plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aksesi.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aksesi.player"
        minSdk = 21
        targetSdk = 35
        versionCode = 9
        versionName = "1.8.0"
    }

    flavorDimensions += "device"
    productFlavors {
        create("googleTv") {
            dimension = "device"
            resValue("string", "app_name", "AKSESI Player")
        }
        create("fireTv") {
            dimension = "device"
            applicationIdSuffix = ".firetv"
            versionNameSuffix = "-firetv"
            resValue("string", "app_name", "AKSESI Player for Fire TV")
        }
    }

    buildTypes {
        release { isMinifyEnabled = false }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
