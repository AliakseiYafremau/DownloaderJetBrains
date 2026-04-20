package downloader.domain.algorithms

import downloader.domain.ByteRange
import downloader.domain.Chunk
import downloader.domain.ChunkPlanner
import downloader.domain.DownloadConfig
import downloader.domain.DownloadPlan
import downloader.domain.ResourceMetadata

class DefaultChunkPlanner : ChunkPlanner {

    override fun plan(metadata: ResourceMetadata, config: DownloadConfig): DownloadPlan {
        if (metadata.contentLength == 0L) {
            return DownloadPlan(metadata, emptyList())
        }

        if (!metadata.supportsRangeReads || metadata.contentLength <= config.minChunkSize) {
            return DownloadPlan(
                metadata,
                listOf(Chunk(0, ByteRange(0, metadata.contentLength - 1)))
            )
        }

        val chunkSize = chooseChunkSize(metadata.contentLength, config)

        val chunks = mutableListOf<Chunk>()
        var start = 0L
        var index = 0

        while (start < metadata.contentLength) {
            val end = minOf(start + chunkSize - 1, metadata.contentLength - 1)
            chunks += Chunk(index++, ByteRange(start, end))
            start = end + 1
        }

        return DownloadPlan(metadata, chunks)
    }

    private fun chooseChunkSize(fileSize: Long, config: DownloadConfig): Long {
        val preferred = maxOf(
            config.minChunkSize,
            kotlin.math.ceil(fileSize.toDouble() / config.maxParallelDownloads).toLong()
        )
        return minOf(preferred, config.maxChunkSize)
    }
}