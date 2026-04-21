package downloader.adapters

import downloader.application.interfaces.ChunkStorage
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Comparator

class FilesystemChunkStorage(
    private val tempDirectory: Path = createTempDirectory()
) : ChunkStorage {

    private val lock = Any()
    private val savedChunkIndices = mutableSetOf<Int>()

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
                moveReplacing(tempFile, destination)
            } catch (exception: Exception) {
                savedChunkIndices.remove(chunkIndex)
                throw AdapterException("Failed to save chunk $chunkIndex", exception)
            } finally {
                Files.deleteIfExists(tempFile)
            }
        }
    }

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

    private fun moveReplacing(from: Path, to: Path) {
        try {
            Files.move(
                from,
                to,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: Exception) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING)
        }
    }

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
        fun createTempDirectory(): Path {
            return try {
                Files.createTempDirectory("downloader-chunks-")
            } catch (exception: Exception) {
                throw AdapterException("Failed to create temp directory for chunks", exception)
            }
        }
    }
}

