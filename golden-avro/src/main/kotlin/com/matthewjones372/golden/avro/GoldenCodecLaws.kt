package com.matthewjones372.golden.avro

import com.github.avrokotlin.avro4k.Avro
import com.matthewjones372.golden.core.GoldenFileManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.KSerializer
import java.io.File

/**
 * Core laws for golden codec testing with Avro.
 * These laws verify that Avro encoders and decoders maintain compatibility over time.
 */
class GoldenCodecLaws<T>(
    private val avro: Avro,
    private val serializer: KSerializer<T>,
    private val goldenFileManager: GoldenFileManager
) {
    /**
     * Golden encoding law: Verifies that encoding a value produces Avro that matches the golden file.
     *
     * Workflow:
     * - If no golden file exists: Creates a "_new" file and fails with instructions
     * - If golden file exists and matches: Test passes
     * - If golden file exists but doesn't match: Creates a "_changed" file and fails with instructions
     */
    fun goldenEncodingLaw(value: T, index: Int): Boolean {
        val goldenFile = goldenFileManager.getGoldenFile(index)
        val newGoldenFile = goldenFileManager.getNewGoldenFile(index)
        val changedGoldenFile = goldenFileManager.getChangedGoldenFile(index)
        val actualBytes = avro.encodeToByteArray(serializer, value)

        return if (!goldenFile.exists()) {
            // First run: create "_new" file
            newGoldenFile.parentFile?.mkdirs()
            newGoldenFile.writeBytes(actualBytes)

            throw AssertionError("""
                Golden file does not exist: ${goldenFile.name}

                A new reference file has been created: ${newGoldenFile.name}

                To accept this as the golden reference:
                  mv ${newGoldenFile.path} ${goldenFile.path}

                Then re-run the test.
            """.trimIndent())
        } else {
            // Subsequent runs: validate against golden file
            val expectedBytes = goldenFile.readBytes()

            if (!actualBytes.contentEquals(expectedBytes)) {
                // Mismatch: create "_changed" file
                changedGoldenFile.writeBytes(actualBytes)

                throw AssertionError("""
                    Golden file mismatch: ${goldenFile.name}

                    The current encoding differs from the golden reference.
                    A changed reference file has been created: ${changedGoldenFile.name}

                    To accept this change as the new golden reference:
                      cp ${changedGoldenFile.path} ${goldenFile.path}

                    If this change was unintended, fix the code and re-run the test.

                    Note: Avro files are binary. Use avro-tools to inspect:
                      avro-tools tojson ${changedGoldenFile.path}
                      avro-tools tojson ${goldenFile.path}
                """.trimIndent())
            }

            true
        }
    }

    /**
     * Golden decoding law: Verifies that golden files can be decoded successfully.
     * This ensures backward compatibility - old serialized data can still be read.
     */
    fun goldenDecodingLaw(index: Int): Boolean {
        val goldenFile = goldenFileManager.getGoldenFile(index)

        if (!goldenFile.exists()) {
            // Skip on first run when golden files don't exist yet
            return true
        }

        val goldenBytes = goldenFile.readBytes()
        val decoded: T = avro.decodeFromByteArray(serializer, goldenBytes)

        // Verify we can decode without errors
        decoded shouldNotBe null

        return true
    }

    /**
     * Round-trip law: Verifies that encode -> decode -> encode produces the same Avro binary.
     * This ensures that the codec is stable and doesn't lose information.
     */
    fun roundTripLaw(value: T): Boolean {
        val bytes1 = avro.encodeToByteArray(serializer, value)
        val decoded: T = avro.decodeFromByteArray(serializer, bytes1)
        val bytes2 = avro.encodeToByteArray(serializer, decoded)

        bytes1.contentEquals(bytes2) shouldBe true

        return true
    }
}
