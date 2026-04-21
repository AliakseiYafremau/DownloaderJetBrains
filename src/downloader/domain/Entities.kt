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

/**
 * Chunk is one fragment of the resource to be downloaded.
 */
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

/**
 * A plan has an ordered list of chunks that must be downloaded.
 */
data class DownloadPlan(
    val fileMetadata: ResourceMetadata,
    val chunks: List<Chunk>,
) {
    init {
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

