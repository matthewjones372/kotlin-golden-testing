package com.matthewjones372.golden.core

import java.io.File

/**
 * Manages golden file storage and retrieval.
 * Golden files are stored in test resources directory with consistent naming.
 *
 * @param typeName The name of the type being tested
 * @param resourcePath The path within test resources (default: "golden")
 * @param fileExtension The file extension for golden files (default: "json")
 */
class GoldenFileManager(
    private val typeName: String,
    private val resourcePath: String = "golden",
    private val fileExtension: String = "json"
) {
    /**
     * Gets the golden file for a specific sample index.
     * Format: src/test/resources/{resourcePath}/{TypeName}_{index}.{fileExtension}
     */
    fun getGoldenFile(index: Int): File {
        val testResourcesDir = File("src/test/resources/$resourcePath")
        return File(testResourcesDir, "${typeName}_${index.toString().padStart(3, '0')}.$fileExtension")
    }

    /**
     * Gets the "_new" golden file for a specific sample index.
     * This file is created when no golden file exists yet (first run).
     * Format: src/test/resources/{resourcePath}/{TypeName}_{index}_new.{fileExtension}
     */
    fun getNewGoldenFile(index: Int): File {
        val testResourcesDir = File("src/test/resources/$resourcePath")
        return File(testResourcesDir, "${typeName}_${index.toString().padStart(3, '0')}_new.$fileExtension")
    }

    /**
     * Gets the "_changed" golden file for a specific sample index.
     * This file is created when the current encoding doesn't match the existing golden file.
     * Format: src/test/resources/{resourcePath}/{TypeName}_{index}_changed.{fileExtension}
     */
    fun getChangedGoldenFile(index: Int): File {
        val testResourcesDir = File("src/test/resources/$resourcePath")
        return File(testResourcesDir, "${typeName}_${index.toString().padStart(3, '0')}_changed.$fileExtension")
    }

    /**
     * Gets all existing golden files for this type.
     */
    fun getAllGoldenFiles(): List<File> {
        val testResourcesDir = File("src/test/resources/$resourcePath")
        if (!testResourcesDir.exists()) {
            return emptyList()
        }

        return testResourcesDir.listFiles { file ->
            file.name.startsWith(typeName) && file.name.endsWith(".$fileExtension")
        }?.toList() ?: emptyList()
    }

    /**
     * Cleans up all golden files for this type.
     * Useful for regenerating golden files from scratch.
     */
    fun cleanGoldenFiles() {
        getAllGoldenFiles().forEach { it.delete() }
    }
}
