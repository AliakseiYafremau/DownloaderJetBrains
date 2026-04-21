package downloader.unit.usecase

import downloader.application.interfaces.ChunkStorage
import downloader.application.interfaces.ParallelChunkDownloader
import downloader.application.interfaces.ResourceGateway
import downloader.application.usecase.DownloadFileUseCase
import downloader.domain.ByteRange
import downloader.domain.Chunk
import downloader.domain.ChunkPlanner
import downloader.domain.DownloadConfig
import downloader.domain.DownloadPlan
import downloader.domain.ResourceMetadata
import downloader.domain.algorithms.Timer
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

class DownloadFileUseCaseTest {
    @Test
    fun `executes all steps in happy path`() {
        // Arrange
        val url = "http://test/file"
        val targetPath = "/tmp/file"
        val config = DownloadConfig(minChunkSize = 1, maxChunkSize = 10, maxParallelDownloads = 2)
        val metadata = ResourceMetadata(supportsRangeReads = true, contentLength = 10)
        val plan = DownloadPlan(metadata, listOf(Chunk(0, ByteRange(0, 9))))

        val resourceGateway = mock<ResourceGateway>()
        val chunkPlanner = mock<ChunkPlanner>()
        val chunkStorage = mock<ChunkStorage>()
        val parallelChunkDownloader = mock<ParallelChunkDownloader>()

        doReturn(metadata).whenever(resourceGateway).fetchMetadata(url)
        doReturn(plan).whenever(chunkPlanner).plan(metadata, config)

        val useCase = DownloadFileUseCase(resourceGateway, chunkPlanner, chunkStorage, parallelChunkDownloader, Timer())

        // Act
        useCase.execute(url, targetPath, config)

        // Assert
        verify(parallelChunkDownloader).downloadChunks(eq(url), eq(plan), eq(2), eq(resourceGateway), eq(chunkStorage))
        verify(chunkStorage).assemble(targetPath)
        verify(chunkStorage).cleanup()
    }

    @Test
    fun `cleanup is always called even if exception occurs`() {
        // Arrange
        val url = "http://test/file"
        val targetPath = "/tmp/file"
        val config = DownloadConfig(minChunkSize = 1, maxChunkSize = 10, maxParallelDownloads = 2)
        val resourceGateway = mock<ResourceGateway>()
        val chunkPlanner = mock<ChunkPlanner>()
        val chunkStorage = mock<ChunkStorage>()
        val parallelChunkDownloader = mock<ParallelChunkDownloader>()

        doThrow(RuntimeException("fail fetch")).whenever(resourceGateway).fetchMetadata(url)

        val useCase = DownloadFileUseCase(resourceGateway, chunkPlanner, chunkStorage, parallelChunkDownloader, Timer())

        // Act and Assert
        try {
            useCase.execute(url, targetPath, config)
        } catch (_: Exception) {}
        verify(chunkStorage).cleanup()
        verify(parallelChunkDownloader, never()).downloadChunks(any(), any(), any(), any(), any())
    }
}
