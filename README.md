# Golden Testing for Kotlin

A Kotlin library for **golden testing** and **property-based testing** of JSON serialization formats, inspired by [zio-json-golden](https://github.com/zio/zio-json/tree/series/2.x/zio-json-golden). Supports Jackson and kotlinx.serialization with Kotest property-based testing.

**Built with ❤️ using [Claude Code](https://claude.com/claude-code)**

## Supported Formats

- **`golden-jackson`**: Jackson JSON serialization ✅
- **`golden-kotlinx-json`**: kotlinx.serialization JSON format ✅
- **`golden-avro`**: Avro binary format (coming soon) 🚧

## Two Testing Approaches

This library provides two complementary testing approaches:

### 1. Golden Testing (`goldenCodecTest`)
- 🎯 **Purpose**: Regression detection and explicit change review
- 📁 **Output**: Small number of reference files (5-10)
- ✅ **Use for**: Detecting unintended serialization changes

### 2. Property-Based Testing (`codecPropertyTest`)
- 🎯 **Purpose**: Comprehensive codec correctness testing
- 🔄 **Output**: No files, just extensive round-trip testing (1000+ iterations)
- ✅ **Use for**: Ensuring codec works correctly across all edge cases

**Recommendation**: Use BOTH! Golden tests catch regressions, property tests prove correctness.

## What is Golden Testing?

Golden testing (also called snapshot testing or characterization testing) is a technique where you:
1. Generate reference files (golden files) from your data structures
2. On subsequent test runs, verify that the encoding/decoding still matches these reference files
3. When you intentionally change your data structures, review and accept the changes explicitly

**Why use golden testing?**
- 🛡️ **Catch unintended changes**: Prevents accidental breaking of serialization compatibility
- 📝 **Explicit change review**: Forces you to review and approve serialization changes
- 🔄 **Backward compatibility**: Ensures old serialized data can still be decoded
- 📊 **Test multiple variations**: Property-based testing generates diverse test cases automatically

## Installation

### Jackson Module

Add to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/matthewjones372/golden-testing")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    testImplementation("com.matthewjones372:golden-jackson:1.0-SNAPSHOT")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")  // For Kotest
}

tasks.test {
    useJUnitPlatform()
}
```

### kotlinx.serialization JSON Module

```kotlin
dependencies {
    testImplementation("com.matthewjones372:golden-kotlinx-json:1.0-SNAPSHOT")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
}
```


## Quick Start

### Jackson Example (Kotest)

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

### Jackson Example (JUnit 5)

If you prefer JUnit 5 over Kotest:

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

**Note:** JUnit tests require `kotlinx-coroutines-test` for the `runTest` wrapper:

```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
```

### kotlinx.serialization JSON Example

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


## Running Tests

### First Run

```bash
./gradlew test
```

**Result:** Test fails with output like:
```
Golden file does not exist: Person_000.json

A new reference file has been created: Person_000_new.json

To accept this as the golden reference:
  mv src/test/resources/golden/Person_000_new.json src/test/resources/golden/Person_000.json

Then re-run the test.
```

### Accept the Golden Files

```bash
# Review the generated files
cat src/test/resources/golden/Person_000_new.json

# Accept them by renaming
for f in src/test/resources/golden/*_new.json; do
    mv "$f" "${f/_new.json/.json}"
done
```

### Re-run the Test

```bash
./gradlew test
```

**Result:** ✅ Test passes!

## The `_new` and `_changed` Workflow

### Workflow 1: First Run (No Golden Files)

1. **Test runs** and generates `_new` files:
   - `Person_000_new.json`
   - `Person_001_new.json`
   - ... (up to `sampleCount`)

2. **Test fails** with clear instructions

3. **Review** the generated files

4. **Accept** by renaming:
   ```bash
   mv src/test/resources/golden/Person_000_new.json src/test/resources/golden/Person_000.json
   ```

5. **Re-run** → ✅ Passes!

### Workflow 2: Data Structure Changed

When you modify your data class:

```kotlin
data class Person(
    val name: String,
    val age: Int,
    val email: String,
    val phoneNumber: String  // ← New field
)
```

1. **Test runs** and generates `_changed` files

2. **Test fails** with diff instructions

3. **Review the diff**:
   ```bash
   diff src/test/resources/golden/Person_000.json src/test/resources/golden/Person_000_changed.json
   ```

4. **If change is intended**, accept it:
   ```bash
   cp src/test/resources/golden/Person_000_changed.json src/test/resources/golden/Person_000.json
   ```

5. **If unintended**, fix your code!

## Configuration Options

### `GoldenCodecTestConfig`

```kotlin
data class GoldenCodecTestConfig(
    val sampleCount: Int = 5,              // Number of golden samples
    val resourcePath: String = "golden",    // Directory under src/test/resources/
    val testRoundTrip: Boolean = true,      // Test round-trip stability
    val testEncoding: Boolean = true,       // Test encoding
    val testDecoding: Boolean = true,       // Test decoding
    val seed: Long? = 1234567890L          // Fixed seed for reproducibility
)
```

### `CodecPropertyTestConfig`

```kotlin
data class CodecPropertyTestConfig(
    val iterations: Int = 1000  // Number of property test iterations
)
```

## Testing Nested Structures

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

## Golden Files in Version Control

**Important:** Commit your golden files to version control!

```bash
git add src/test/resources/golden/
git commit -m "Add golden test files for Person"
```

Benefits:
- **Code review**: Reviewers can see serialization changes in PRs
- **History**: Track how your format evolves over time
- **CI**: Ensures tests pass in CI with the same reference files

## FAQ

**Q: How many samples should I generate?**
A: Start with 5. Increase to 10-20 if you have optional fields or complex variations.

**Q: Can I use this with both JUnit and Kotest?**
A: Yes! Kotest is recommended but JUnit 5 works with the `runTest` wrapper from `kotlinx-coroutines-test`.

**Q: Do I need to commit `_new` and `_changed` files?**
A: No! Only commit the final `.json` files. The `_new` and `_changed` files are temporary.

**Q: Which module should I use?**
A:
- `golden-jackson` for Jackson JSON
- `golden-kotlinx-json` for kotlinx.serialization JSON

**Q: Can I add support for other formats?**
A: Yes! The `golden-core` module provides format-agnostic file management. Create a new module following the pattern of existing modules.

## Comparison with Other Approaches

| Approach | Manual JSON | Snapshot Testing | Golden Testing (this lib) |
|----------|-------------|------------------|---------------------------|
| Explicit review | ❌ No | ⚠️ Sometimes | ✅ Always |
| Multiple variations | ❌ Hard | ⚠️ Manual | ✅ Automatic (property testing) |
| Backward compat | ❌ Not tested | ❌ Not tested | ✅ Tested (decoding law) |
| Round-trip testing | ❌ Not tested | ❌ Not tested | ✅ Tested (round-trip law) |
| Clear workflow | ⚠️ Manual | ⚠️ Auto-update | ✅ `_new`/`_changed` workflow |

## License

MIT License

## Credits

Inspired by:
- [zio-json-golden](https://github.com/zio/zio-json/tree/series/2.x/zio-json-golden)
- [circe-golden](https://github.com/circe/circe-golden)

**Built with ❤️ using [Claude Code](https://claude.com/claude-code)**
