pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "pocket-beach"
// The core (bsp) is plain Kotlin and builds anywhere a JDK runs. Pass
// -PcoreOnly to leave out the Android app, as on a laptop with no Android SDK:
//   gradlew :bsp:test -PcoreOnly
include(":bsp")
if (!providers.gradleProperty("coreOnly").isPresent) include(":app")
// The companion's trial against the real model, on a laptop (see trial/build.gradle.kts).
if (providers.gradleProperty("trial").isPresent) include(":trial")
