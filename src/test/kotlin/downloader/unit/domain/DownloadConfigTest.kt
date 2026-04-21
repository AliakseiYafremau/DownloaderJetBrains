package downloader.unit.domain

import downloader.domain.DownloadConfig
import downloader.domain.InvalidDataException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DownloadConfigTest {
    @Test
    fun `creates config when values are valid`() {
        // Arrange
        val minChunkSize = 2L
        val maxChunkSize = 8L
        val maxParallelDownloads = 3

        // Act
        val result = DownloadConfig(
            minChunkSize = minChunkSize,
            maxChunkSize = maxChunkSize,
            maxParallelDownloads = maxParallelDownloads,
        )

        // Assert
        assertEquals(minChunkSize, result.minChunkSize)
        assertEquals(maxChunkSize, result.maxChunkSize)
        assertEquals(maxParallelDownloads, result.maxParallelDownloads)
    }

    @Test
    fun `creates config when max chunk size is null`() {
        // Arrange
        val minChunkSize = 2L
        val maxParallelDownloads = 2

        // Act
        val result = DownloadConfig(
            minChunkSize = minChunkSize,
            maxChunkSize = null,
            maxParallelDownloads = maxParallelDownloads,
        )

        // Assert
        assertEquals(minChunkSize, result.minChunkSize)
        assertEquals(null, result.maxChunkSize)
        assertEquals(maxParallelDownloads, result.maxParallelDownloads)
    }

    @Test
    fun `min chunk size cannot be non positive`() {
        // Arrange
        val minChunkSize = 0L

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            DownloadConfig(minChunkSize = minChunkSize, maxChunkSize = 8L, maxParallelDownloads = 2)
        }
    }

    @Test
    fun `max chunk size cannot be non positive`() {
        // Arrange
        val maxChunkSize = 0L

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            DownloadConfig(minChunkSize = 1L, maxChunkSize = maxChunkSize, maxParallelDownloads = 2)
        }
    }

    @Test
    fun `min chunk size cannot be greater than max chunk size`() {
        // Arrange
        val minChunkSize = 10L
        val maxChunkSize = 5L

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            DownloadConfig(minChunkSize = minChunkSize, maxChunkSize = maxChunkSize, maxParallelDownloads = 2)
        }
    }

    @Test
    fun `max parallel downloads cannot be non positive`() {
        // Arrange
        val maxParallelDownloads = 0

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            DownloadConfig(minChunkSize = 1L, maxChunkSize = 8L, maxParallelDownloads = maxParallelDownloads)
        }
    }
}

