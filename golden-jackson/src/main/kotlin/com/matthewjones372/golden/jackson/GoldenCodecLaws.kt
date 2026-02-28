package com.matthewjones372.golden.jackson

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.flipkart.zjsonpatch.JsonDiff
import com.matthewjones372.golden.core.GoldenFileManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jline.utils.WCWidth

/**
 * Core laws for golden codec testing with Jackson.
 * These laws verify that JSON encoders and decoders maintain compatibility over time.
 */
class GoldenCodecLaws<T>(
    private val mapper: ObjectMapper,
    private val typeReference: TypeReference<T>,
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
        val actualJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)

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

                // Generate simple diff summary
                val diffSummary = generateDiffSummary(expectedJson, actualJson)

                throw AssertionError("""
                    Golden file mismatch: ${goldenFile.name}

                    The current encoding differs from the golden reference.
                    A changed reference file has been created: ${changedGoldenFile.name}

                    Changes detected:
                    $diffSummary

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
        val decoded: T = mapper.readValue(goldenJson, typeReference)

        // Verify we can decode without errors
        decoded shouldNotBe null

        return true
    }

    /**
     * Round-trip law: Verifies that encode -> decode -> encode produces the same JSON.
     * This ensures that the codec is stable and doesn't lose information.
     */
    fun roundTripLaw(value: T): Boolean {
        val json1 = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
        val decoded: T = mapper.readValue(json1, typeReference)
        val json2 = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(decoded)

        normalizeJson(json1) shouldBe normalizeJson(json2)

        return true
    }

    /**
     * Normalizes JSON by removing insignificant whitespace differences.
     */
    private fun normalizeJson(json: String): String {
        return mapper.readTree(json).toString()
    }

    /**
     * Generates a human-readable summary of differences between two JSON documents.
     */
    private fun generateDiffSummary(expectedJson: String, actualJson: String): String {
        val expectedTree = mapper.readTree(expectedJson)
        val actualTree = mapper.readTree(actualJson)
        val patch = JsonDiff.asJson(expectedTree, actualTree)

        val added = mutableListOf<String>()
        val removed = mutableListOf<String>()
        val modified = mutableListOf<String>()

        patch.forEach { operation ->
            val op = operation.get("op")?.asText()
            val path = operation.get("path")?.asText().orEmpty()
            when (op) {
                "add" -> added.add(path)
                "remove" -> removed.add(path)
                "replace" -> modified.add(path)
            }
        }

        val entries = buildList<Pair<String, String>> {
            if (added.isNotEmpty()) add(green("Added:") to "✨ ${added.joinToString(", ")}")
            if (removed.isNotEmpty()) add(red("Removed:") to "🗑️  ${removed.joinToString(", ")}")
            if (modified.isNotEmpty()) add(yellow("Modified:") to "✏️  ${modified.joinToString(", ")}")
        }

        if (entries.isEmpty()) {
            // leading newline matters here too
            return "\n${yellow("⚠️  (structural changes detected)")}"
        }

        val maxWidth = entries.maxOf { displayWidth(it.first) }

        val body = buildString {
            for ((label, value) in entries) {
                val pad = " ".repeat((maxWidth - displayWidth(label)).coerceAtLeast(0) + 1)
                appendLine("$label$pad$value")
            }
        }.trimEnd()

        // IMPORTANT: leading newline so the runner indents all lines consistently
        return "\n$body"
    }

    // ANSI is zero width; unicode width via wcwidth
    private fun displayWidth(s: String): Int {
        var w = 0
        var i = 0
        while (i < s.length) {
            if (s[i] == '\u001B' && i + 1 < s.length && s[i + 1] == '[') {
                i += 2
                while (i < s.length && s[i] !in 'A'..'Z' && s[i] !in 'a'..'z') i++
                if (i < s.length) i++
                continue
            }
            val cp = Character.codePointAt(s, i)
            w += WCWidth.wcwidth(cp).coerceAtLeast(0)
            i += Character.charCount(cp)
        }
        return w
    }

    private fun green(text: String) = "\u001B[32m$text\u001B[0m"
    private fun red(text: String) = "\u001B[31m$text\u001B[0m"
    private fun yellow(text: String) = "\u001B[33m$text\u001B[0m"
}
