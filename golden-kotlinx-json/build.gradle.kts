plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.21"
}

group = "com.matthewjones372"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Core module
    implementation(project(":golden-core"))

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Kotlin reflection
    implementation(kotlin("reflect"))

    // Kotest - needed in main code for golden testing API
    implementation("io.kotest:kotest-assertions-core:5.9.1")
    implementation("io.kotest:kotest-property:5.9.1")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
