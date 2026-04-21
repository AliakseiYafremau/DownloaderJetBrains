package downloader.adapters

import downloader.application.interfaces.ChunkStorage
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Comparator

/**
 * File-system based [ChunkStorage] implementation.
 *
 * Stores chunks as temporary files by index and assembles them into a target file
 * in ascending chunk-index order.
 */
class FilesystemChunkStorage(
    private val tempDirectory: Path = createTempDirectory()
) : ChunkStorage {

    private val lock = Any()
    private val savedChunkIndices = mutableSetOf<Int>()

    /**
     * Persists chunk bytes under the given index.
     *
     * Fails on duplicate indices to prevent accidental overwrites from concurrent downloads.
     */
    override fun save(chunkIndex: Int, bytes: ByteArray) {
        if (chunkIndex < 0) {
            throw AdapterException("chunkIndex must be >= 0")
        }

        synchronized(lock) {
            if (!savedChunkIndices.add(chunkIndex)) {
                throw AdapterException("chunk $chunkIndex already saved")
            }

            Files.createDirectories(tempDirectory)
            val tempFile = Files.createTempFile(tempDirectory, "chunk-$chunkIndex-", ".tmp")
            val destination = chunkPath(chunkIndex)

            try {
                Files.write(tempFile, bytes)
                Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING)
            } catch (exception: Exception) {
                savedChunkIndices.remove(chunkIndex)
                throw AdapterException("Failed to save chunk $chunkIndex", exception)
            } finally {
                Files.deleteIfExists(tempFile)
            }
        }
    }

    /**
     * Builds the final file at [targetPath] by concatenating stored chunks in index order.
     *
     * Requires chunk indices to start at 0 and be contiguous.
     */
    override fun assemble(targetPath: String) {
        val target = validateTargetPath(targetPath)

        try {
            synchronized(lock) {
                val orderedIndices = savedChunkIndices.sorted()
                validateContiguousIndices(orderedIndices)

                target.parent?.let { Files.createDirectories(it) }

                Files.newOutputStream(
                    target,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                ).use { output ->
                    for (index in orderedIndices) {
                        val part = chunkPath(index)
                        if (!Files.exists(part)) {
                            throw AdapterException("missing chunk file for index $index")
                        }
                        Files.newInputStream(part).use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            throw AdapterException("Failed to assemble chunks into $targetPath", exception)
        }
    }

    /**
     * Deletes all temporary chunk files and resets in-memory chunk tracking.
     */
    override fun cleanup() {
        try {
            synchronized(lock) {
                if (Files.exists(tempDirectory)) {
                    Files.walk(tempDirectory).use { paths ->
                        paths.sorted(Comparator.reverseOrder()).forEach { path ->
                            Files.deleteIfExists(path)
                        }
                    }
                }
                savedChunkIndices.clear()
            }
        } catch (exception: Exception) {
            throw AdapterException("Failed to cleanup chunk storage", exception)
        }
    }

    private fun chunkPath(chunkIndex: Int): Path = tempDirectory.resolve("chunk-$chunkIndex.part")

    /**
     * Validates that chunk indices form a contiguous sequence starting from 0.
     */
    private fun validateContiguousIndices(indices: List<Int>) {
        if (indices.isEmpty()) {
            return
        }

        if (indices.first() != 0) {
            throw AdapterException("chunk indices must start with 0")
        }
        for (i in 1 until indices.size) {
            if (indices[i] != indices[i - 1] + 1) {
                throw AdapterException("chunk indices must be contiguous")
            }
        }
    }

    /**
     * Validates and parses the target path where the final file will be assembled.
     *
     * Rejects blank paths, invalid paths, directories, and non-regular existing files.
     */
    private fun validateTargetPath(targetPath: String): Path {
        if (targetPath.isBlank()) {
            throw AdapterException("targetPath must not be blank")
        }

        val path = try {
            Path.of(targetPath)
        } catch (exception: InvalidPathException) {
            throw AdapterException("Invalid targetPath: $targetPath", exception)
        }

        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                throw AdapterException("targetPath points to a directory: $targetPath")
            }
            if (!Files.isRegularFile(path)) {
                throw AdapterException("targetPath must point to a regular file: $targetPath")
            }
        }

        return path
    }

    private companion object {
        /**
         * Creates a dedicated temporary directory for chunk files.
         */
        fun createTempDirectory(): Path {
            return try {
                Files.createTempDirectory("downloader-chunks-")
            } catch (exception: Exception) {
                throw AdapterException("Failed to create temp directory for chunks", exception)
            }
        }
    }
}
