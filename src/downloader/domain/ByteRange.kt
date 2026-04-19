package downloader.domain

data class ByteRange(
    val start: Long,
    val end: Long,
) {
    init {
        require(start >= 0) { "startInclusive must be >= 0" }
        require(end >= start) { "endInclusive must be >= startInclusive" }
    }
}


