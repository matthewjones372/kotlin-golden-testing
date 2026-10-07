# Golden Testing for Kotlin

[![Maven Central](https://img.shields.io/maven-central/v/io.github.matthewjones372/golden-core)](https://central.sonatype.com/artifact/io.github.matthewjones372/golden-core)
[![GitHub release](https://img.shields.io/github/v/release/matthewjones372/kotlin-golden-testing)](https://github.com/matthewjones372/kotlin-golden-testing/releases)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

Golden testing and property-based testing for JSON codecs in Kotlin, based on [zio-json-golden](https://github.com/zio/zio-json/tree/series/2.x/zio-json-golden). It works with Jackson and kotlinx.serialization, and uses Kotest generators.

This library checks how your own types are serialized. If you want to know
whether a change to an HTTP API would break its callers, that is a different
question, and [Pelican](https://github.com/matthewjones372/pelican)'s
`pelican-test-golden` answers it: it keeps one golden file per endpoint and
fails only when a change would break someone already calling the service
([its guide](https://github.com/matthewjones372/pelican/blob/main/docs/golden-testing.md)).

## Modules

- `golden-jackson`: Jackson
- `golden-kotlinx-json`: kotlinx.serialization JSON
- `golden-core`: format-agnostic golden file handling, used by both

## Two kinds of test

`goldenCodecTest` writes a handful of sample values (5 by default) to reference files and checks on each run that encoding and decoding still match them. Use it to catch serialization changes you didn't mean to make.

`codecPropertyTest` writes no files. It round-trips many generated values (1000 by default) to check the codec is consistent with itself.

They cover different things, so it's worth using both.

## What golden testing is

Golden testing (also called snapshot or characterization testing) works like this:

1. Generate reference files from your data types.
2. On later runs, check that encoding and decoding still match those files.
3. When you change a type on purpose, review the new output and accept it.

Because the reference files are committed, a change to the serialized format shows up in code review, and old data is checked to still decode.

## Installation

### Jackson

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.github.matthewjones372:golden-jackson:1.0.6")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}
```

### kotlinx.serialization

```kotlin
dependencies {
    testImplementation("io.github.matthewjones372:golden-kotlinx-json:1.0.6")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
}
```

## Examples

### Jackson with Kotest

```kotlin
import com.matthewjones372.golden.jackson.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string

data class Person(
    val name: String,
    val age: Int,
    val email: String
)

class PersonGoldenTest : FunSpec({
    val mapper = createGoldenTestObjectMapper()

    test("test person golden codec") {
        goldenCodecTest(
            mapper = mapper,
            arb = Arb.bind(
                Arb.string(1..50),
                Arb.int(0..120),
                Arb.string(5..100),
                ::Person
            ),
            config = GoldenCodecTestConfig(sampleCount = 5)
        )
    }

    test("test person codec properties") {
        codecPropertyTest(
            mapper = mapper,
            arb = Arb.bind(
                Arb.string(1..50),
                Arb.int(0..120),
                Arb.string(5..100),
                ::Person
            ),
            config = CodecPropertyTestConfig(iterations = 1000)
        )
    }
})
```

### Jackson with JUnit 5

```kotlin
import com.matthewjones372.golden.jackson.*
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PersonGoldenTest {
    private val mapper = createGoldenTestObjectMapper()

    @Test
    fun `test person golden codec`() = runTest {
        goldenCodecTest(
            mapper = mapper,
            arb = Arb.bind(
                Arb.string(1..50),
                Arb.int(0..120),
                Arb.string(5..100),
                ::Person
            ),
            config = GoldenCodecTestConfig(sampleCount = 5)
        )
    }

    @Test
    fun `test person codec properties`() = runTest {
        codecPropertyTest(
            mapper = mapper,
            arb = Arb.bind(
                Arb.string(1..50),
                Arb.int(0..120),
                Arb.string(5..100),
                ::Person
            ),
            config = CodecPropertyTestConfig(iterations = 1000)
        )
    }
}
```

JUnit tests need `kotlinx-coroutines-test` for `runTest`:

```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
```

### kotlinx.serialization

```kotlin
import com.matthewjones372.golden.kotlinx.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import kotlinx.serialization.Serializable

@Serializable
data class Person(
    val name: String,
    val age: Int,
    val email: String
)

class PersonGoldenTest : FunSpec({
    val json = createGoldenTestJson()

    test("test person golden codec") {
        goldenCodecTest(
            json = json,
            serializer = Person.serializer(),
            typeName = "Person",
            arb = Arb.bind(
                Arb.string(1..50),
                Arb.int(0..120),
                Arb.string(5..100),
                ::Person
            ),
            config = GoldenCodecTestConfig(sampleCount = 5)
        )
    }
})
```

### Nested types

```kotlin
data class Company(
    val name: String,
    val employees: List<Person>,
    val founded: Int
)

test("test company golden codec") {
    val personArb = Arb.bind(
        Arb.string(1..50),
        Arb.int(0..120),
        Arb.string(5..100),
        ::Person
    )

    val companyArb = Arb.bind(
        Arb.string(1..100),
        Arb.list(personArb, 0..10),
        Arb.int(1800..2024),
        ::Company
    )

    goldenCodecTest(
        mapper = mapper,
        arb = companyArb,
        config = GoldenCodecTestConfig(sampleCount = 3)
    )
}
```

## Working with golden files

Golden files live under `src/test/resources/golden/` by default, one per sample: `Person_000.json`, `Person_001.json` and so on.

### First run

With no golden files yet, the test writes `_new` files and fails:

```
Golden file does not exist: Person_000.json

A new reference file has been created: Person_000_new.json

To accept this as the golden reference:
  mv src/test/resources/golden/Person_000_new.json src/test/resources/golden/Person_000.json

Then re-run the test.
```

Look over the generated files, then accept them all:

```bash
for f in src/test/resources/golden/*_new.json; do
    mv "$f" "${f/_new.json/.json}"
done
```

The next run passes.

### When a type changes

If the output no longer matches, for example after adding a field, the test writes a `_changed` file next to the golden one and fails. Diff the two:

```bash
diff src/test/resources/golden/Person_000.json src/test/resources/golden/Person_000_changed.json
```

If the change is what you wanted, copy it over the golden file:

```bash
cp src/test/resources/golden/Person_000_changed.json src/test/resources/golden/Person_000.json
```

If it isn't, fix the code.

### Version control

Commit the golden `.json` files. Don't commit the `_new` or `_changed` files; they only exist until you accept or reject them.

## Configuration

```kotlin
data class GoldenCodecTestConfig(
    val sampleCount: Int = 5,              // Number of golden samples
    val resourcePath: String = "golden",   // Directory under src/test/resources/
    val testRoundTrip: Boolean = true,     // Test round-trip stability
    val testEncoding: Boolean = true,      // Test encoding
    val testDecoding: Boolean = true,      // Test decoding
    val seed: Long? = 1234567890L          // Fixed seed so samples are reproducible
)

data class CodecPropertyTestConfig(
    val iterations: Int = 1000             // Number of property test iterations
)
```

Five samples is a reasonable start. Types with many optional fields or variants may need 10 to 20.

## Other formats

`golden-core` handles the files and knows nothing about JSON. A new format is a new module that follows the pattern of `golden-jackson` and `golden-kotlinx-json`.

## Publishing

Versions come from git tags via the [axion-release-plugin](https://github.com/allegro/axion-release-plugin). A tagged commit gets that version (`1.0.6`); commits after a tag get a snapshot (`1.0.7-SNAPSHOT`).

Pushing a `v*` tag runs the GitHub Actions workflow, which builds, signs and publishes to Maven Central:

```bash
git tag v1.0.7
git push origin v1.0.7
```

The workflow needs these repository secrets: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `GPG_PRIVATE_KEY` (ASCII-armored) and `GPG_PASSPHRASE`.

To publish from a local machine, put the same values in `~/.gradle/gradle.properties`:

```properties
mavenCentralUsername=your-sonatype-username
mavenCentralPassword=your-sonatype-password
signingInMemoryKey=<your-gpg-private-key-ascii-armored>
signingInMemoryKeyPassword=your-gpg-passphrase
```

and run:

```bash
./gradlew publishAllPublicationsToMavenCentralRepository
```

## License

MIT. See [LICENSE](LICENSE).

## Credits

Based on [zio-json-golden](https://github.com/zio/zio-json/tree/series/2.x/zio-json-golden) and [circe-golden](https://github.com/circe/circe-golden).
