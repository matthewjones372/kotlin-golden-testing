plugins {
    kotlin("jvm")
}

group = "com.matthewjones372"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Core module
    implementation(project(":golden-core"))

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

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
