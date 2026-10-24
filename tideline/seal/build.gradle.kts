import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The seal, as plain Kotlin with no Android in it, so the app carries it and
// any machine can check it against the Pscale tool (spine:tideline 6.2).
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
    // Argon2id, XSalsa20 and Poly1305; nothing in it reaches the network.
    implementation("org.bouncycastle:bcprov-jdk18on:1.86")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("sealCheck") {
    description = "Seals or opens one trial line with TIDELINE_READING_KEY."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("life.matthewbushell.tideline.seal.SealCheckKt")
    standardInput = System.`in`
}
