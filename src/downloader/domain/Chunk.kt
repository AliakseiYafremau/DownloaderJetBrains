package downloader.domain

data class Chunk(
    val index: Int,
    val range: ByteRange,
) {
    init {
        if (index < 0) {
            throw InvalidDataException("chunk index must be >= 0")
        }
    }
}
