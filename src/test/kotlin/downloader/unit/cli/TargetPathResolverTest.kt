package downloader.unit.cli

import downloader.cli.TargetPathResolver
import downloader.domain.InvalidDataException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TargetPathResolverTest {
    @Test
    fun `resolves file name inside existing directory`(@TempDir tempDir: Path) {
        // Arrange
        val url = "http://localhost:8080/test-1gb.bin"

        // Act
        val resolved = TargetPathResolver.resolve(url, tempDir.toString())

        // Assert
        assertEquals(tempDir.resolve("test-1gb.bin"), Path.of(resolved))
    }

    @Test
    fun `resolves file name inside directory path ending with separator`(@TempDir tempDir: Path) {
        // Arrange
        val url = "http://localhost:8080/file.bin"
        val nestedDirectory = tempDir.resolve("nested")
        val directoryArg = nestedDirectory.toString() + File.separator

        // Act
        val resolved = TargetPathResolver.resolve(url, directoryArg)

        // Assert
        assertEquals(nestedDirectory.resolve("file.bin"), Path.of(resolved))
    }

    @Test
    fun `keeps explicit file target path unchanged`(@TempDir tempDir: Path) {
        // Arrange
        val url = "http://localhost:8080/file.bin"
        val targetFile = tempDir.resolve("custom-output.bin")

        // Act
        val resolved = TargetPathResolver.resolve(url, targetFile.toString())

        // Assert
        assertEquals(targetFile, Path.of(resolved))
    }

    @Test
    fun `uses fallback name when url path has no filename`(@TempDir tempDir: Path) {
        // Arrange
        val url = "http://localhost:8080/files/"

        // Act
        val resolved = TargetPathResolver.resolve(url, tempDir.toString())

        // Assert
        assertEquals(tempDir.resolve("downloaded.file"), Path.of(resolved))
    }

    @Test
    fun `blank target path fails`() {
        // Arrange
        val url = "http://localhost:8080/file.bin"

        // Act
        val exception = assertFailsWith<InvalidDataException> {
            TargetPathResolver.resolve(url, "   ")
        }

        // Assert
        assertEquals("targetPath must not be blank", exception.message)
    }
}
