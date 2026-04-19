package downloader.domain

data class Chunk(
    val index: Int,
    val range: ByteRange,
)