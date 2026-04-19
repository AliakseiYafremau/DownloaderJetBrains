package downloader.application.usecase

import downloader.application.interfaces.ChunkGateway
import downloader.domain.ChunkPlanner
import downloader.domain.algorithms.DefaultChunkPlanner
import downloader.domain.DownloadConfig
import downloader.domain.DownloadPlan
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class DownloadFileUseCase(val chunkGateway: ChunkGateway, val chunkPlanner: ChunkPlanner) {

    fun execute(
        url: String,
        targetPath: String,
        config: DownloadConfig,
    ) {
        // Step 1: Fetch metadata
        val metadata = chunkGateway.fetchMetadata(url)

        // Step 2: Plan chunks
        val plan = chunkPlanner.plan(metadata, config)

        // Step 3: Download chunks in parallel
        val downloadedChunks = downloadChunksInParallel(url, plan, config.maxParallelDownloads)

        // Step 4: Assemble and save file
        assembleAndSaveFile(downloadedChunks, targetPath, metadata.contentLength)
    }

    private fun downloadChunksInParallel(
        url: String,
        plan: DownloadPlan,
        maxParallel: Int
    ): Map<Int, ByteArray> {
        val chunks = ConcurrentHashMap<Int, ByteArray>()
        val executor = Executors.newFixedThreadPool(maxParallel)

        try {
            val futures = plan.chunks.map { chunk ->
                executor.submit {
                    val bytes = chunkGateway.downloadRange(url, chunk.range)
                    chunks[chunk.index] = bytes
                }
            }

            // Wait for all downloads to complete
            futures.forEach { it.get() }
        } finally {
            executor.shutdown()
        }

        return chunks
    }

    private fun assembleAndSaveFile(
        chunks: Map<Int, ByteArray>,
        targetPath: String,
        @Suppress("UNUSED_PARAMETER") totalSize: Long
    ) {
        val file = File(targetPath)
        file.parentFile?.mkdirs()

        file.outputStream().use { output ->
            for (i in 0 until chunks.size) {
                val chunkData = chunks[i] ?: throw IllegalStateException("Missing chunk $i")
                output.write(chunkData)
            }
        }
    }
}