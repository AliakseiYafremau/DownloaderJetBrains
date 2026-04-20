package downloader.adapters

import downloader.application.interfaces.ChunkGateway
import downloader.application.interfaces.ChunkStorage
import downloader.application.interfaces.ParallelChunkDownloader
import downloader.domain.DownloadPlan
import java.util.concurrent.CompletionService
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors

class DefaultParallelChunkDownloader : ParallelChunkDownloader {

    override fun downloadChunks(
        url: String,
        plan: DownloadPlan,
        maxParallel: Int,
        chunkGateway: ChunkGateway,
        chunkStorage: ChunkStorage,
    ) {
        require(maxParallel > 0) { "maxParallel must be > 0" }

        if (plan.chunks.isEmpty()) {
            return
        }

        val executor = Executors.newFixedThreadPool(minOf(maxParallel, plan.chunks.size))
        val completionService: CompletionService<Unit> = ExecutorCompletionService(executor)

        try {
            val submittedTasks = plan.chunks.size

            for (chunk in plan.chunks) {
                completionService.submit<Unit> {
                    val bytes = chunkGateway.downloadRange(url, chunk.range)
                    chunkStorage.save(chunk.index, bytes)
                }
            }

            repeat(submittedTasks) {
                try {
                    completionService.take().get()
                } catch (exception: Exception) {
                    throw unwrapExecutionException(exception)
                }
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun unwrapExecutionException(exception: Exception): RuntimeException {
        val cause = exception.cause
        return when (cause) {
            is RuntimeException -> cause
            is Error -> throw cause
            null -> RuntimeException(exception)
            else -> RuntimeException(cause)
        }
    }
}

