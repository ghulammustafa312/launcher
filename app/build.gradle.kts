plugins {
    alias(libs.plugins.android.application)
}

// Single source for namespace and applicationId. See appPackage in gradle.properties.
val appPackage = providers.gradleProperty("appPackage").get()

android {
    namespace = appPackage
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = appPackage
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    // Pure Java module: the AAB contains no native libraries, so it is 64-bit safe.
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.splashscreen)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.biometric)
}
