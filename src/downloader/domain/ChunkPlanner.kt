package downloader.domain

interface ChunkPlanner {
    fun plan(metadata: ResourceMetadata, config: DownloadConfig): DownloadPlan
}