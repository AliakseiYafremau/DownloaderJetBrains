package downloader.domain

data class ResourceMetadata(
    val supportsRangeReads: Boolean,
    val contentLength: Long,
)
