package downloader.domain

data class ByteRange(
    val start: Long,
    val end: Long,
) {
    init {
        if (start < 0) {
            throw InvalidDataException("startInclusive must be >= 0")
        }
        if (end < start) {
            throw InvalidDataException("endInclusive must be >= startInclusive")
        }
    }
}
