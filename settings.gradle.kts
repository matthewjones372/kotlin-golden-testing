plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "golden-testing"

include("golden-core")
include("golden-jackson")
include("golden-kotlinx-json")
