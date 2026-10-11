import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

val androidSdkDirectory =
    listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")
        .firstNotNullOfOrNull { System.getenv(it)?.let(::File) }
val latestInstalledStableSdk =
    androidSdkDirectory
        ?.resolve("platforms")
        ?.listFiles()
        ?.mapNotNull { platform ->
            Regex("android-(\\d+)").matchEntire(platform.name)?.groupValues?.get(1)?.toIntOrNull()
        }
        ?.maxOrNull()
val selectedCompileSdk =
    providers.gradleProperty("androidCompileSdk").orNull?.toInt()
        ?: providers.environmentVariable("ANDROID_COMPILE_SDK").orNull?.toInt()
        ?: latestInstalledStableSdk
        ?: 36
val selectedTargetSdk =
    providers.gradleProperty("androidTargetSdk").orNull?.toInt()
        ?: providers.environmentVariable("ANDROID_TARGET_SDK").orNull?.toInt()
        ?: selectedCompileSdk
val releaseStorePath = providers.environmentVariable("KBRIDGE_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("KBRIDGE_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("KBRIDGE_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("KBRIDGE_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning =
    listOf(
        releaseStorePath,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

android {
    namespace = "com.kbridge.ai"
    compileSdk = selectedCompileSdk

    defaultConfig {
        applicationId = "com.kbridge.ai"
        minSdk = 31
        targetSdk = selectedTargetSdk
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStorePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
