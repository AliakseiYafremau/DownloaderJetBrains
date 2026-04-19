package downloader.application.usecase

import downloader.application.interfaces.ChunkGateway
import downloader.domain.ChunkPlanner
import downloader.domain.ChunkStorage
import downloader.domain.DownloadConfig
import downloader.domain.ParallelChunkDownloader

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
            // Step 1: Fetch metadata
            val metadata = chunkGateway.fetchMetadata(url)

            // Step 2: Plan chunks
            val plan = chunkPlanner.plan(metadata, config)

            // Step 3: Download chunks in parallel
            parallelChunkDownloader.downloadChunks(
                url = url,
                plan = plan,
                maxParallel = config.maxParallelDownloads,
                chunkGateway = chunkGateway,
                chunkStorage = chunkStorage
            )

            // Step 4: Assemble file
            chunkStorage.assemble(targetPath)
        } finally {
            chunkStorage.cleanup()
        }
    }
}