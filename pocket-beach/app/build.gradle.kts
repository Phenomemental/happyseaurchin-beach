plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The release key never lives in the repo: CI writes it from the repository
// secrets into a file and names it here (see .github/workflows/pocket-beach.yml).
val releaseKeystore: String? = System.getenv("POCKET_KEYSTORE_FILE")

android {
    namespace = "com.phenomemental.pocketbeach"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.phenomemental.pocketbeach"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("POCKET_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("POCKET_KEY_ALIAS")
                keyPassword = System.getenv("POCKET_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    sourceSets {
        // The teaching shelf, byte for byte as bsp-mcp-server bundles it.
        getByName("main").assets.srcDir(rootProject.file("shelf"))
        // The golden calls, replayed on the device by the instrumented test.
        getByName("androidTest").assets.srcDir(rootProject.file("conformance/golden"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":bsp"))
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.13.0")

    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
