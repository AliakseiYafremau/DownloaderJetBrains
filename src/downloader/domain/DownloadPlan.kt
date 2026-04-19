package downloader.domain

data class DownloadPlan(
    val fileMetadata: ResourceMetadata,
    val chunks: List<Chunk>,
)