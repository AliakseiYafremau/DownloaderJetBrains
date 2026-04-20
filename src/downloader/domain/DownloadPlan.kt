package downloader.domain

data class DownloadPlan(
    val fileMetadata: ResourceMetadata,
    val chunks: List<Chunk>,
) {
    init {
        if (fileMetadata.contentLength < 0) {
            throw InvalidDataException("contentLength must be >= 0")
        }

        if (chunks.isEmpty()) {
            if (fileMetadata.contentLength != 0L) {
                throw InvalidDataException("non-empty file must contain at least one chunk")
            }
        } else {
            val sorted = chunks.sortedBy { it.index }
            for ((expectedIndex, chunk) in sorted.withIndex()) {
                if (chunk.index != expectedIndex) {
                    throw InvalidDataException("chunk indices must be contiguous and start at 0")
                }
            }

            var expectedStart = 0L
            for (chunk in sorted) {
                if (chunk.range.start != expectedStart) {
                    throw InvalidDataException("chunk ranges must be contiguous and start at 0")
                }
                if (chunk.range.end >= fileMetadata.contentLength) {
                    throw InvalidDataException("chunk range exceeds file content length")
                }
                expectedStart = chunk.range.end + 1

            }

            if (expectedStart != fileMetadata.contentLength) {
                throw InvalidDataException("chunks must fully cover file content length")
            }
        }
    }
}
