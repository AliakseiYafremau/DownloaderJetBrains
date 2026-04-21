package downloader.unit.algorithms

import downloader.domain.*
import downloader.domain.algorithms.DefaultChunkPlanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultChunkPlannerTest {
    @Test
    fun `plans single chunk for zero-length file`() {
        // Arrange
        val planner = DefaultChunkPlanner()
        val metadata = ResourceMetadata(supportsRangeReads = true, contentLength = 0)
        val config = DownloadConfig(minChunkSize = 1, maxChunkSize = 10, maxParallelDownloads = 2)

        // Act
        val plan = planner.plan(metadata, config)

        // Assert
        assertEquals(0, plan.chunks.size)
    }

    @Test
    fun `plans single chunk if server does not support range reads`() {
        // Arrange
        val planner = DefaultChunkPlanner()
        val metadata = ResourceMetadata(supportsRangeReads = false, contentLength = 100)
        val config = DownloadConfig(minChunkSize = 10, maxChunkSize = 50, maxParallelDownloads = 2)

        // Act
        val plan = planner.plan(metadata, config)

        // Assert
        assertEquals(1, plan.chunks.size)
        assertEquals(ByteRange(0, 99), plan.chunks[0].range)
    }

    @Test
    fun `plans single chunk if file is smaller than minChunkSize`() {
        // Arrange
        val planner = DefaultChunkPlanner()
        val metadata = ResourceMetadata(supportsRangeReads = true, contentLength = 5)
        val config = DownloadConfig(minChunkSize = 10, maxChunkSize = 50, maxParallelDownloads = 2)

        // Act
        val plan = planner.plan(metadata, config)

        // Assert
        assertEquals(1, plan.chunks.size)
        assertEquals(ByteRange(0, 4), plan.chunks[0].range)
    }

    @Test
    fun `plans multiple chunks for large file with range support`() {
        // Arrange
        val planner = DefaultChunkPlanner()
        val metadata = ResourceMetadata(supportsRangeReads = true, contentLength = 100)
        val config = DownloadConfig(minChunkSize = 10, maxChunkSize = 30, maxParallelDownloads = 4)

        // Act
        val plan = planner.plan(metadata, config)

        // Assert
        assertTrue(plan.chunks.size > 1)
        assertEquals(0, plan.chunks.first().range.start)
        assertEquals(99, plan.chunks.last().range.end)
        // Chunks must be contiguous
        for (i in 1 until plan.chunks.size) {
            assertEquals(plan.chunks[i-1].range.end + 1, plan.chunks[i].range.start)
        }
    }
}

