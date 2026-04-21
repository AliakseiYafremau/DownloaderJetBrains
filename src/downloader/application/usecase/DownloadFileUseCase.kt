package downloader.application.usecase

import downloader.application.interfaces.ResourceGateway
import downloader.domain.ChunkPlanner
import downloader.application.interfaces.ChunkStorage
import downloader.domain.DownloadConfig
import downloader.application.interfaces.ParallelChunkDownloader

import downloader.domain.algorithms.Timer

class DownloadFileUseCase(
    val resourceGateway: ResourceGateway,
    val chunkPlanner: ChunkPlanner,
    val chunkStorage: ChunkStorage,
    val parallelChunkDownloader: ParallelChunkDownloader,
    val timer: Timer
) {

    fun execute(
        url: String,
        targetPath: String,
        config: DownloadConfig,
    ): Long {
        timer.start()
        try {
            val metadata = resourceGateway.fetchMetadata(url)

            val plan = chunkPlanner.plan(metadata, config)

            parallelChunkDownloader.downloadChunks(
                url = url,
                plan = plan,
                maxParallel = config.maxParallelDownloads,
                resourceGateway = resourceGateway,
                chunkStorage = chunkStorage
            )

            chunkStorage.assemble(targetPath)
            return timer.elapsedMillis()
        } finally {
            chunkStorage.cleanup()
        }
    }
}