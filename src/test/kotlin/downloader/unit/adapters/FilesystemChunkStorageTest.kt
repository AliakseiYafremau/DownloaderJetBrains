package downloader.unit.adapters

import downloader.adapters.AdapterException
import downloader.adapters.FilesystemChunkStorage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilesystemChunkStorageTest {
    @Test
    fun `assembles chunks saved out of order`(@TempDir tempDir: Path) {
        // Arrange
        val storage = FilesystemChunkStorage(tempDir.resolve("chunks"))
        val target = tempDir.resolve("target.bin")
        storage.save(1, byteArrayOf(3, 4))
        storage.save(0, byteArrayOf(1, 2))
        storage.save(2, byteArrayOf(5))

        // Act
        storage.assemble(target.toString())

        // Assert
        assertTrue(Files.exists(target), "Target file should exist after assemble")
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), Files.readAllBytes(target))
    }

    @Test
    fun `assemble without chunks creates empty file`(@TempDir tempDir: Path) {
        // Arrange
        val storage = FilesystemChunkStorage(tempDir.resolve("chunks"))
        val target = tempDir.resolve("target.bin")

        // Act
        storage.assemble(target.toString())

        // Assert
        assertTrue(Files.exists(target), "Target file should be created")
        assertEquals(0L, Files.size(target), "Target file should be empty")
    }

    @Test
    fun `cleanup removes temporary storage`(@TempDir tempDir: Path) {
        // Arrange
        val storageDirectory = tempDir.resolve("chunks")
        val storage = FilesystemChunkStorage(storageDirectory)
        storage.save(0, byteArrayOf(1, 2, 3))
        assertTrue(Files.exists(storageDirectory), "Storage directory should exist before cleanup")

        // Act
        storage.cleanup()

        // Assert
        assertFalse(Files.exists(storageDirectory), "Storage directory should be removed")
    }

    @Test
    fun `saving duplicate chunk index fails`(@TempDir tempDir: Path) {
        // Arrange
        val storage = FilesystemChunkStorage(tempDir.resolve("chunks"))
        storage.save(0, byteArrayOf(1))

        // Act
        val exception = assertFailsWith<AdapterException> {
            storage.save(0, byteArrayOf(2))
        }

        // Assert
        assertTrue(
            exception.message?.contains("already saved") == true,
            "Duplicate chunk save should fail with clear message"
        )
    }
}
