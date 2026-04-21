package downloader.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResourceMetadataTest {
    @Test
    fun `creates metadata when content length is valid`() {
        // Arrange
        val supportsRangeReads = true
        val contentLength = 128L

        // Act
        val result = ResourceMetadata(
            supportsRangeReads = supportsRangeReads,
            contentLength = contentLength,
        )

        // Assert
        assertEquals(supportsRangeReads, result.supportsRangeReads)
        assertEquals(contentLength, result.contentLength)
    }

    @Test
    fun `content length cannot be negative`() {
        // Arrange
        val invalidLength = -1L

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            ResourceMetadata(supportsRangeReads = true, contentLength = invalidLength)
        }
    }
}

