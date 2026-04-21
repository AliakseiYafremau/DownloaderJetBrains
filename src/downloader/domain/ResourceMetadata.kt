package downloader.domain

data class ResourceMetadata(
    val supportsRangeReads: Boolean,
    val contentLength: Long,
) {
    init {
        if (contentLength < 0) {
            throw InvalidDataException("contentLength must be >= 0")
        }
    }
}
