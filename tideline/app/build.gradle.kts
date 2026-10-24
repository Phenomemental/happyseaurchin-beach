// The phone's half of Tideline (spine:tideline 7.11, 7.12). Kotlin is built
// into the Android plugin from version 9, so no Kotlin plugin is applied here.
plugins {
    id("com.android.application")
}

// The signing key and its password come from the GitHub Actions secrets
// (spine:tideline 7.17), never from the repo; without them the release build
// is left unsigned.
val signingKey: String? = System.getenv("TIDELINE_SIGNING_KEY_FILE")

android {
    namespace = "life.matthewbushell.tideline"
    compileSdk = 37

    defaultConfig {
        applicationId = "life.matthewbushell.tideline"
        minSdk = 29
        targetSdk = 37
        // Each GitHub build counts up, so a new APK installs over the last.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1
        versionName = "stage 6.3"
    }

    signingConfigs {
        if (signingKey != null) {
            create("release") {
                storeFile = file(signingKey)
                storeType = "pkcs12"
                storePassword = System.getenv("TIDELINE_SIGNING_PASSWORD")
                keyAlias = "tideline"
                keyPassword = System.getenv("TIDELINE_SIGNING_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":seal"))

    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
}
