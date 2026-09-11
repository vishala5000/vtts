plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.vtts.app"

    compileSdk = 35

    defaultConfig {
        applicationId = "com.vtts.app"

        minSdk = 26
        targetSdk = 35

        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    androidResources {
        noCompress += listOf(
            "onnx",
            "bin"
        )
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }

        resources {
            excludes += setOf(
                "META-INF/LICENSE",
                "META-INF/NOTICE",
                "META-INF/*.kotlin_module"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(
        files("libs/sherpa-onnx-1.13.7.aar")
    )

    implementation(
        "androidx.appcompat:appcompat:1.7.1"
    )
}
