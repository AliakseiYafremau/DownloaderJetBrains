package downloader.application.usecase

import downloader.application.interfaces.ResourceGateway
import downloader.domain.ChunkPlanner
import downloader.application.interfaces.ChunkStorage
import downloader.domain.DownloadConfig
import downloader.application.interfaces.ParallelChunkDownloader

import downloader.domain.algorithms.Timer

/**
 * Orchestrates the end-to-end file download flow.
 *
 * Fetches metadata, builds a chunk plan, downloads chunks in parallel using the plan, assembles the
 * resulting file, and guarantees temporary storage cleanup.
 */
class DownloadFileUseCase(
    val resourceGateway: ResourceGateway,
    val chunkPlanner: ChunkPlanner,
    val chunkStorage: ChunkStorage,
    val parallelChunkDownloader: ParallelChunkDownloader,
    val timer: Timer
) {

    /**
     * Executes the download flow for [url] into [targetPath] using [config].
     *
     * @return elapsed execution time in milliseconds.
     */
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
