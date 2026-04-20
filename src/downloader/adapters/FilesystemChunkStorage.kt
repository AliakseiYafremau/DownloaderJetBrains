package downloader.adapters

import downloader.application.interfaces.ChunkStorage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Comparator

class FilesystemChunkStorage(
    private val tempDirectory: Path = Files.createTempDirectory("downloader-chunks-")
) : ChunkStorage {

    private val lock = Any()
    private val savedChunkIndices = mutableSetOf<Int>()

    override fun save(chunkIndex: Int, bytes: ByteArray) {
        require(chunkIndex >= 0) { "chunkIndex must be >= 0" }

        synchronized(lock) {
            check(savedChunkIndices.add(chunkIndex)) { "chunk $chunkIndex already saved" }

            Files.createDirectories(tempDirectory)
            val tempFile = Files.createTempFile(tempDirectory, "chunk-$chunkIndex-", ".tmp")
            val destination = chunkPath(chunkIndex)

            try {
                Files.write(tempFile, bytes)
                moveReplacing(tempFile, destination)
            } catch (exception: Exception) {
                savedChunkIndices.remove(chunkIndex)
                throw exception
            } finally {
                Files.deleteIfExists(tempFile)
            }
        }
    }

    override fun assemble(targetPath: String) {
        val target = Path.of(targetPath)

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
                    check(Files.exists(part)) { "missing chunk file for index $index" }
                    Files.newInputStream(part).use { input ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    override fun cleanup() {
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

        require(indices.first() == 0) { "chunk indices must start with 0" }
        for (i in 1 until indices.size) {
            require(indices[i] == indices[i - 1] + 1) { "chunk indices must be contiguous" }
        }
    }
}

