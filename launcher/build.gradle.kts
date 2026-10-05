plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.sandboxr.launcher"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("boolean", "IS_STUDIO_BUILD", "true")
        buildConfigField("boolean", "WIDGETS_ENABLED", "true")
        buildConfigField("boolean", "NOTIFICATION_DOTS_ENABLED", "true")
        buildConfigField("boolean", "IS_DEBUG_DEVICE", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = false
        checkReleaseBuilds = false
    }

    sourceSets {
        named("main") {
            val customJavaDirs = listOf(
                "src/main/java",
                "src/main/src",
                "src/main/quickstep/src",
                "src/main/shared/src",
                "src/main/modules/concurrent/src",
                "src/main/modules/appfunctions/src",
                "src/main/modules/widgetpicker/src",
                "src/main/src_plugins"
            )
            val customResDirs = listOf(
                "src/main/res",
                "src/main/quickstep/res",
                "src/main/modules/widgetpicker/res"
            )
            val customAidlDirs = listOf(
                "src/main/aidl",
                "src/main/quickstep/aidl"
            )

            java.directories.addAll(customJavaDirs)
            kotlin.directories.addAll(customJavaDirs)
            res.directories.addAll(customResDirs)
            aidl.directories.addAll(customAidlDirs)
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "1536m"
            it.forkEvery = 15
        }
    }
}

kotlin {
    jvmToolchain(21)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                register("java") {
                    option("lite")
                }
                register("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(project(":virtual-core"))
    implementation(project(":network"))
    implementation(project(":aidl"))

    // AndroidX & Architecture Components
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.dynamicanimation)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.slice.view)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.window)
    implementation(libs.google.material)
    implementation(libs.airbnb.lottie)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Dagger DI & JSR-330
    implementation(libs.dagger)
    implementation(libs.javax.inject)
    ksp(libs.dagger.compiler)

    // Protobuf Lite
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlin.lite)

    // Guava (provides ListeningExecutorService used by ExecutorsModule)
    implementation(libs.guava)

    // Testing
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

