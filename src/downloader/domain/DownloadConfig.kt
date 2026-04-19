package downloader.domain

data class DownloadConfig(
    val minChunkSize: Long,
    val maxChunkSize: Long,
    val maxParallelDownloads: Int,
)
