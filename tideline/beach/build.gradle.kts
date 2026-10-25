import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The phone's calls to the beach, as plain Kotlin, so the app carries them and
// any machine can try them against a trial block (spine:tideline 6.4, 7.14).
plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    // Android carries org.json itself; only a computer needs it supplied.
    compileOnly("org.json:json:20260814")

    testImplementation(project(":seal"))
    testImplementation("org.json:json:20260814")
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("beachCheck") {
    description = "Lands sealed test lines on a trial block with the app's own calls."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("life.matthewbushell.tideline.beach.BeachCheckKt")
    standardInput = System.`in`
}
