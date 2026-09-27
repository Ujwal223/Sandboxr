plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ujwal.sandboxr"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ujwal.sandboxr"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    room {
        schemaDirectory("$projectDir/schemas")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildFeatures {
        compose = true
    }

    // ByteHook and ShadowHook AARs bundle their .so both as a Prefab CMake import
    // and as raw jniLibs. pickFirst resolves the duplicate-file merge conflict.
    packaging {
        jniLibs {
            pickFirsts += setOf(
                "lib/arm64-v8a/libbytehook.so",
                "lib/armeabi-v7a/libbytehook.so",
                "lib/arm64-v8a/libshadowhook.so",
                "lib/armeabi-v7a/libshadowhook.so"
            )
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":launcher"))
    implementation(project(":virtual-core"))
    implementation(project(":network"))
    implementation(project(":aidl"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
