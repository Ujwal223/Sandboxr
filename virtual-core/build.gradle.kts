plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sandboxr.virtual"
    compileSdk = 37
    ndkVersion = "28.2.13676358"

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17", "-fvisibility=hidden", "-Wall")
                arguments(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
                )
            }
        }

        ndk {
            // ShadowHook and ByteHook ship ARM prebuilts only — x86_64 is not supported
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        prefab = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hidden.api.bypass)
    implementation(libs.bytedance.shadowhook)
    implementation(libs.bytedance.bytehook)

    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

