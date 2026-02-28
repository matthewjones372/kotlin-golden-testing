package com.matthewjones372.golden.avro

import com.github.avrokotlin.avro4k.Avro
import com.matthewjones372.golden.core.GoldenFileManager
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import kotlinx.serialization.KSerializer

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
 * Main entry point for golden codec testing with Avro.
 * This function performs property-based golden testing on a type T.
 *
 * @param avro The Avro instance to use for serialization/deserialization
 * @param serializer The KSerializer for type T
 * @param typeName The name of the type (used for file naming)
 * @param arb The Kotest Arb generator for producing test values
 * @param config Configuration for the golden test
 *
 * Usage:
 * ```
 * test("test person golden codec") {
 *     goldenCodecTest(
 *         avro = Avro,
 *         serializer = Person.serializer(),
 *         typeName = "Person",
 *         arb = Arb.bind(Arb.string(), Arb.int(), ::Person)
 *     )
 * }
 * ```
 */
@OptIn(io.kotest.common.ExperimentalKotest::class)
suspend fun <T> goldenCodecTest(
    avro: Avro,
    serializer: KSerializer<T>,
    typeName: String,
    arb: Arb<T>,
    config: GoldenCodecTestConfig = GoldenCodecTestConfig()
) {
    val fileManager = GoldenFileManager(typeName, config.resourcePath, "avro")
    val laws = GoldenCodecLaws(avro, serializer, fileManager)

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
