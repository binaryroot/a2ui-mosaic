plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    kotlin("plugin.compose") version "2.1.10"
    application
}

group = "org.a2ui.mosaic"
version = "0.1.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    // Mosaic
    implementation("com.jakewharton.mosaic:mosaic-runtime:0.18.0")

    // Compose Runtime (JetBrains version matching Mosaic 0.18.0)
    implementation("org.jetbrains.compose.runtime:runtime:1.8.2")

    // Kotlinx Serialization for JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

application {
    mainClass.set("org.a2ui.mosaic.sample.MainKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

// Connect stdin to the run task so Mosaic can detect TTY.
// Note: This helps but may not fully provide a TTY — use installDist for best results.
tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
