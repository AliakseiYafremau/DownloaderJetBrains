package downloader.domain

/**
 * Responsible for determining how to split a file into chunks for parallel downloading.
 *
 * The planning logic considers the file size, server capabilities (e.g., support for range reads),
 * and the configured minimum and maximum chunk sizes to create an optimal download plan.
 */
interface ChunkPlanner {
    fun plan(metadata: ResourceMetadata, config: DownloadConfig): DownloadPlan
}