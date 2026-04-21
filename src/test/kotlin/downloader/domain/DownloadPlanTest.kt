package downloader.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DownloadPlanTest {
    @Test
    fun `creates download plan when chunks fully cover content contiguously`() {
        // Arrange
        val metadata = ResourceMetadata(supportsRangeReads = true, contentLength = 6)
        val chunks = listOf(
            Chunk(index = 0, range = ByteRange(start = 0, end = 2)),
            Chunk(index = 1, range = ByteRange(start = 3, end = 5)),
        )

        // Act
        val result = DownloadPlan(fileMetadata = metadata, chunks = chunks)

        // Assert
        assertEquals(metadata, result.fileMetadata)
        assertEquals(chunks, result.chunks)
    }

    @Test
    fun `creates download plan when file is empty and chunks are empty`() {
        // Arrange
        val metadata = ResourceMetadata(supportsRangeReads = true, contentLength = 0)
        val chunks = emptyList<Chunk>()

        // Act
        val result = DownloadPlan(fileMetadata = metadata, chunks = chunks)

        // Assert
        assertEquals(metadata, result.fileMetadata)
        assertEquals(chunks, result.chunks)
    }

    @Test
    fun `non empty file cannot have no chunks`() {
        // Arrange
        val metadata = ResourceMetadata(supportsRangeReads = true, contentLength = 1)
        val chunks = emptyList<Chunk>()

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            DownloadPlan(fileMetadata = metadata, chunks = chunks)
        }
    }

    @Test
    fun `chunk indices cannot be non contiguous from zero`() {
        // Arrange
        val metadata = ResourceMetadata(supportsRangeReads = true, contentLength = 6)
        val chunks = listOf(
            Chunk(index = 0, range = ByteRange(start = 0, end = 2)),
            Chunk(index = 2, range = ByteRange(start = 3, end = 5)),
        )

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            DownloadPlan(fileMetadata = metadata, chunks = chunks)
        }
    }

    @Test
    fun `chunk ranges cannot be non contiguous from zero`() {
        // Arrange
        val metadata = ResourceMetadata(supportsRangeReads = true, contentLength = 6)
        val chunks = listOf(
            Chunk(index = 0, range = ByteRange(start = 1, end = 3)),
            Chunk(index = 1, range = ByteRange(start = 4, end = 5)),
        )

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            DownloadPlan(fileMetadata = metadata, chunks = chunks)
        }
    }

    @Test
    fun `chunk end cannot exceed file content length`() {
        // Arrange
        val metadata = ResourceMetadata(supportsRangeReads = true, contentLength = 6)
        val chunks = listOf(
            Chunk(index = 0, range = ByteRange(start = 0, end = 2)),
            Chunk(index = 1, range = ByteRange(start = 3, end = 6)),
        )

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            DownloadPlan(fileMetadata = metadata, chunks = chunks)
        }
    }

    @Test
    fun `chunks cannot leave file content length uncovered`() {
        // Arrange
        val metadata = ResourceMetadata(supportsRangeReads = true, contentLength = 7)
        val chunks = listOf(
            Chunk(index = 0, range = ByteRange(start = 0, end = 2)),
            Chunk(index = 1, range = ByteRange(start = 3, end = 5)),
        )

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            DownloadPlan(fileMetadata = metadata, chunks = chunks)
        }
    }
}

