package downloader.domain

data class DownloadConfig(
    val minChunkSize: Long,
    val maxChunkSize: Long,
    val maxParallelDownloads: Int,
) {
    init {
        if (minChunkSize <= 0) {
            throw InvalidDataException("minChunkSize must be > 0")
        }
        if (maxChunkSize <= 0) {
            throw InvalidDataException("maxChunkSize must be > 0")
        }
        if (minChunkSize > maxChunkSize) {
            throw InvalidDataException("minChunkSize must be <= maxChunkSize")
        }
        if (maxParallelDownloads <= 0) {
            throw InvalidDataException("maxParallelDownloads must be > 0")
        }
    }
}
