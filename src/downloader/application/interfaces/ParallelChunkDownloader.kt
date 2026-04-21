package downloader.application.interfaces

import downloader.domain.DownloadPlan

/**
 * Responsible for downloading chunks in parallel.
 *
 * Does NOT:
 * - plan chunks
 * - assemble files
 *
 * Does:
 * - execute download tasks in parallel
 * - coordinate with ResourceGateway to fetch data
 * - coordinate with ChunkStorage to save data
 *
 * Implementation requires:
 * - ResourceGateway: to fetch chunk data
 * - ChunkStorage: to persist chunk data
 */
interface ParallelChunkDownloader {
    /**
     * Download all chunks from the plan in parallel.
     * Uses method parameters for gateway and storage dependencies.
     *
     * @param url resource URL to download from
     * @param plan download plan containing chunks and their ranges
     * @param maxParallel maximum number of concurrent downloads
     * @param resourceGateway gateway used to fetch chunk bytes
     * @param chunkStorage storage used to persist downloaded chunks
     */
    fun downloadChunks(
        url: String,
        plan: DownloadPlan,
        maxParallel: Int,
        resourceGateway: ResourceGateway,
        chunkStorage: ChunkStorage
    )
}