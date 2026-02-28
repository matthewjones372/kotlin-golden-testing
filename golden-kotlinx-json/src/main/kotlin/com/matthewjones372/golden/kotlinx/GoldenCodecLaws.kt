package com.matthewjones372.golden.kotlinx

import com.matthewjones372.golden.core.GoldenFileManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Core laws for golden codec testing with kotlinx.serialization.
 * These laws verify that JSON encoders and decoders maintain compatibility over time.
 */
class GoldenCodecLaws<T>(
    private val json: Json,
    private val serializer: KSerializer<T>,
    private val goldenFileManager: GoldenFileManager
) {
    /**
     * Golden encoding law: Verifies that encoding a value produces JSON that matches the golden file.
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
        val actualJson = json.encodeToString(serializer, value)

        return if (!goldenFile.exists()) {
            // First run: create "_new" file
            newGoldenFile.parentFile?.mkdirs()
            newGoldenFile.writeText(actualJson)

            throw AssertionError("""
                Golden file does not exist: ${goldenFile.name}

                A new reference file has been created: ${newGoldenFile.name}

                To accept this as the golden reference:
                  mv ${newGoldenFile.path} ${goldenFile.path}

                Then re-run the test.
            """.trimIndent())
        } else {
            // Subsequent runs: validate against golden file
            val expectedJson = goldenFile.readText()
            val actualNormalized = normalizeJson(actualJson)
            val expectedNormalized = normalizeJson(expectedJson)

            if (actualNormalized != expectedNormalized) {
                // Mismatch: create "_changed" file
                changedGoldenFile.writeText(actualJson)

                throw AssertionError("""
                    Golden file mismatch: ${goldenFile.name}

                    The current encoding differs from the golden reference.
                    A changed reference file has been created: ${changedGoldenFile.name}

                    To accept this change as the new golden reference:
                      cp ${changedGoldenFile.path} ${goldenFile.path}

                    If this change was unintended, fix the code and re-run the test.
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

        val goldenJson = goldenFile.readText()
        val decoded: T = json.decodeFromString(serializer, goldenJson)

        // Verify we can decode without errors
        decoded shouldNotBe null

        return true
    }

    /**
     * Round-trip law: Verifies that encode -> decode -> encode produces the same JSON.
     * This ensures that the codec is stable and doesn't lose information.
     */
    fun roundTripLaw(value: T): Boolean {
        val json1 = json.encodeToString(serializer, value)
        val decoded: T = json.decodeFromString(serializer, json1)
        val json2 = json.encodeToString(serializer, decoded)

        normalizeJson(json1) shouldBe normalizeJson(json2)

        return true
    }

    /**
     * Normalizes JSON by removing insignificant whitespace differences.
     */
    private fun normalizeJson(jsonString: String): String {
        // Parse and re-encode to normalize formatting
        val element = json.parseToJsonElement(jsonString)
        return json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), element)
    }
}
