package downloader.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChunkTest {
    @Test
    fun `creates chunk when index and range are valid`() {
        // Arrange
        val index = 0
        val range = ByteRange(start = 0, end = 4)

        // Act
        val result = Chunk(index = index, range = range)

        // Assert
        assertEquals(index, result.index)
        assertEquals(range, result.range)
    }

    @Test
    fun `index cannot be negative`() {
        // Arrange
        val invalidIndex = -1
        val range = ByteRange(start = 0, end = 4)

        // Act and Assert
        assertFailsWith<InvalidDataException> {
            Chunk(index = invalidIndex, range = range)
        }
    }
}

