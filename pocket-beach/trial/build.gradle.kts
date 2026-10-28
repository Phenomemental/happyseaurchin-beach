import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The companion's trial on a laptop: the hello world, asked of the same model
// with the same engine (LiteRT-LM's desktop build of the API the phone uses)
// and the same instruction the phone gives it, run as many times as asked.
// Included only with -Ptrial:
//   gradlew :trial:run -Ptrial --args="<model.litertlm> <runs>"
plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":bsp"))
    // -PlitertlmVersion=… tries another release of the engine on the laptop first.
    implementation("com.google.ai.edge.litertlm:litertlm-jvm:${findProperty("litertlmVersion") ?: "0.18.0"}")
}

application {
    // -PmainClass=… runs another of the trial's programs (Engines: what the engine allows).
    mainClass.set(findProperty("mainClass")?.toString() ?: "com.phenomemental.pocketbeach.trial.TrialKt")
}

tasks.named<JavaExec>("run") {
    systemProperty("pocket.shelf", rootProject.file("shelf").path)
    // One cache for the engine's prepared weights (about 0.8 GB), reused run to run.
    findProperty("engineCache")?.let { systemProperty("pocket.cache", it.toString()) }
    findProperty("only")?.let { systemProperty("pocket.only", it.toString()) }
    workingDir = rootProject.projectDir
}
