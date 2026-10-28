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
    implementation("com.google.ai.edge.litertlm:litertlm-jvm:0.18.0")
}

application {
    mainClass.set("com.phenomemental.pocketbeach.trial.TrialKt")
}

tasks.named<JavaExec>("run") {
    systemProperty("pocket.shelf", rootProject.file("shelf").path)
    workingDir = rootProject.projectDir
}
