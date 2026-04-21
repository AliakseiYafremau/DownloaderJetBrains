package downloader.unit.domain

import downloader.domain.ByteRange
import downloader.domain.InvalidDataException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ByteRangeTest {
    @Test
    fun `creates byte range when start and end are valid`() {
        // Arrange
        val start = 0L
        val end = 9L

        // Act
        val result = ByteRange(start = start, end = end)

        // Assert
        assertEquals(start, result.start)
        assertEquals(end, result.end)
    }

    @Test
    fun `creates byte range when start equals end`() {
        // Arrange
        val start = 5L
        val end = 5L

        // Act
        val result = ByteRange(start = start, end = end)

        // Assert
        assertEquals(start, result.start)
        assertEquals(end, result.end)
    }

    @Test
    fun `start cannot be negative`() {
        // Arrange
        val start = -1L
        val end = 3L

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            ByteRange(start = start, end = end)
        }
    }

    @Test
    fun `end cannot be lower than start`() {
        // Arrange
        val start = 10L
        val end = 9L

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            ByteRange(start = start, end = end)
        }
    }
}

