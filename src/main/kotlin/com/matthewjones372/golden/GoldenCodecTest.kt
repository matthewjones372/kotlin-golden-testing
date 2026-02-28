package com.matthewjones372.golden

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.RandomSource
import io.kotest.property.checkAll

/**
 * Configuration for golden codec tests.
 */
data class GoldenCodecTestConfig(
    val sampleCount: Int = 5,
    val resourcePath: String = "golden",
    val testRoundTrip: Boolean = true,
    val testEncoding: Boolean = true,
    val testDecoding: Boolean = true,
    val seed: Long? = 1234567890L // Fixed seed for reproducible golden tests
)

/**
 * Main entry point for golden codec testing.
 * This function performs property-based golden testing on a type T.
 *
 * @param mapper The Jackson ObjectMapper to use for serialization/deserialization
 * @param arb The Kotest Arb generator for producing test values
 * @param config Configuration for the golden test
 *
 * Usage:
 * ```
 * @Test
 * fun `test person golden codec`() = runTest {
 *     goldenCodecTest(
 *         mapper = jacksonObjectMapper(),
 *         arb = Arb.bind(Arb.string(), Arb.int(), ::Person)
 *     )
 * }
 * ```
 */
@OptIn(io.kotest.common.ExperimentalKotest::class)
suspend inline fun <reified T> goldenCodecTest(
    mapper: ObjectMapper,
    arb: Arb<T>,
    config: GoldenCodecTestConfig = GoldenCodecTestConfig()
) {
    val fileManager = GoldenFileManager.create<T>(config.resourcePath)
    val laws = GoldenCodecLaws(mapper, fileManager)

    // Track the sample index for golden file naming
    var sampleIndex = 0

    // Use fixed seed for reproducible tests
    val random = if (config.seed != null) {
        RandomSource.seeded(config.seed)
    } else {
        RandomSource.default()
    }

    // Collect all failures so we can generate all _new/_changed files before failing
    val failures = mutableListOf<Pair<Int, String>>()

    for (i in 0 until config.sampleCount) {
        val value = arb.sample(random).value
        try {
            // Test golden encoding law
            if (config.testEncoding) {
                laws.goldenEncodingLaw(value, i)
            }

            // Test golden decoding law
            if (config.testDecoding) {
                laws.goldenDecodingLaw(i)
            }

            // Test round-trip law
            if (config.testRoundTrip) {
                laws.roundTripLaw(value)
            }
        } catch (e: AssertionError) {
            failures.add(i to e.message.orEmpty())
        }
    }

    // If there were any failures, report them all
    if (failures.isNotEmpty()) {
        val failureMessages = failures.joinToString("\n\n") { (index, message) ->
            "Sample $index:\n$message"
        }
        throw AssertionError("Golden test failures:\n\n$failureMessages")
    }
}

/**
 * Variant that accepts a custom GoldenFileManager for more control.
 */
@OptIn(io.kotest.common.ExperimentalKotest::class)
suspend fun <T> goldenCodecTest(
    mapper: ObjectMapper,
    arb: Arb<T>,
    fileManager: GoldenFileManager<T>,
    config: GoldenCodecTestConfig = GoldenCodecTestConfig()
) {
    val laws = GoldenCodecLaws(mapper, fileManager)

    var sampleIndex = 0

    // Use fixed seed for reproducible tests
    val random = if (config.seed != null) {
        RandomSource.seeded(config.seed)
    } else {
        RandomSource.default()
    }

    // Collect all failures so we can generate all _new/_changed files before failing
    val failures = mutableListOf<Pair<Int, String>>()

    for (i in 0 until config.sampleCount) {
        val value = arb.sample(random).value
        try {
            if (config.testEncoding) {
                laws.goldenEncodingLaw(value, i)
            }

            if (config.testDecoding) {
                laws.goldenDecodingLaw(i)
            }

            if (config.testRoundTrip) {
                laws.roundTripLaw(value)
            }
        } catch (e: AssertionError) {
            failures.add(i to e.message.orEmpty())
        }
    }

    // If there were any failures, report them all
    if (failures.isNotEmpty()) {
        val failureMessages = failures.joinToString("\n\n") { (index, message) ->
            "Sample $index:\n$message"
        }
        throw AssertionError("Golden test failures:\n\n$failureMessages")
    }
}
