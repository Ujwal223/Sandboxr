plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sandboxr.network"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

// Gradle task to execute Go unit tests and compile Go firestack engine
tasks.register<Exec>("testGoFirestack") {
    workingDir = file("src/main/go")
    // Use go from PATH (or ~/.local/bin/go)
    commandLine("go", "test", "-v", "./...")
}

// Hook Go testing into Gradle preBuild / check pipeline
tasks.named("preBuild") {
    dependsOn("testGoFirestack")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
