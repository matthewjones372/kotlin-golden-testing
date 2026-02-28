plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.21"
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    // Core module - exposed to users
    api(project(":golden-core"))

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Jackson for JSON diff (not exposed to users)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.flipkart.zjsonpatch:zjsonpatch:0.4.16")

    // JLine for proper unicode width calculation
    implementation("org.jline:jline:3.27.1")

    // Kotlin reflection
    implementation(kotlin("reflect"))

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
