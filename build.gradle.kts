plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    id("maven-publish")
}

allprojects {
    group = "io.github.matthewjones372"
    version = "1.0.1"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    tasks.withType<Test> {
        useJUnitPlatform()

        // Always show test output (disable caching for visibility)
        outputs.upToDateWhen { false }

        // Show test results in real-time
        testLogging {
            events("passed", "skipped", "failed", "standardOut", "standardError")
            showStandardStreams = false
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }

        // Capture project name at configuration time to avoid deprecation warning
        val projectName = project.name

        // Print summary after tests
        afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
            if (desc.parent == null) {
                println("\n$projectName Test Results: ${result.resultType}")
                println("  Tests run: ${result.testCount}")
                println("  Passed: ${result.successfulTestCount}")
                println("  Failed: ${result.failedTestCount}")
                println("  Skipped: ${result.skippedTestCount}")
            }
        }))
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/matthewjones372/kotlin-golden-testing")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }

        publications {
            create<MavenPublication>("gpr") {
                from(components["java"])

                pom {
                    name.set(project.name)
                    description.set("Kotlin golden testing library for ${project.name}")
                    url.set("https://github.com/matthewjones372/kotlin-golden-testing")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("matthewjones372")
                            name.set("Matthew Jones")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/matthewjones372/kotlin-golden-testing.git")
                        developerConnection.set("scm:git:ssh://github.com/matthewjones372/kotlin-golden-testing.git")
                        url.set("https://github.com/matthewjones372/kotlin-golden-testing")
                    }
                }
            }
        }
    }
}
