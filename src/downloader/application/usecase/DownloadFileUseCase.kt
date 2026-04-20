package downloader.application.usecase

import downloader.application.interfaces.ChunkGateway
import downloader.domain.ChunkPlanner
import downloader.application.interfaces.ChunkStorage
import downloader.domain.DownloadConfig
import downloader.application.interfaces.ParallelChunkDownloader

class DownloadFileUseCase(
    val chunkGateway: ChunkGateway,
    val chunkPlanner: ChunkPlanner,
    val chunkStorage: ChunkStorage,
    val parallelChunkDownloader: ParallelChunkDownloader
) {

    fun execute(
        url: String,
        targetPath: String,
        config: DownloadConfig,
    ) {
        try {
            val metadata = chunkGateway.fetchMetadata(url)

            val plan = chunkPlanner.plan(metadata, config)

            parallelChunkDownloader.downloadChunks(
                url = url,
                plan = plan,
                maxParallel = config.maxParallelDownloads,
                chunkGateway = chunkGateway,
                chunkStorage = chunkStorage
            )

            chunkStorage.assemble(targetPath)
        } finally {
            chunkStorage.cleanup()
        }
    }
}