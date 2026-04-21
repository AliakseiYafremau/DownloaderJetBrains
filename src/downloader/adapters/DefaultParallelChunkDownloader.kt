package downloader.adapters

import downloader.application.interfaces.ResourceGateway
import downloader.application.interfaces.ChunkStorage
import downloader.application.interfaces.ParallelChunkDownloader
import downloader.domain.DownloadPlan
import java.util.concurrent.Callable
import java.util.concurrent.CompletionService
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors

/**
 * Default [ParallelChunkDownloader] implementation backed by a fixed thread pool.
 *
 * Submits one task per chunk, waits for all tasks to complete, and fails fast if any
 * task ends with an exception.
 */
class DefaultParallelChunkDownloader : ParallelChunkDownloader {

    /**
     * Downloads all chunks from [plan] in parallel using at most [maxParallel] worker threads.
     *
     * On the first task failure, wraps and rethrows the cause as [AdapterException]
     * (except JVM [Error], which is rethrown as-is).
     */
    override fun downloadChunks(
        url: String,
        plan: DownloadPlan,
        maxParallel: Int,
        resourceGateway: ResourceGateway,
        chunkStorage: ChunkStorage,
    ) {
        if (maxParallel <= 0) {
            throw AdapterException("maxParallel must be > 0")
        }

        if (plan.chunks.isEmpty()) {
            return
        }

        val executor = Executors.newFixedThreadPool(minOf(maxParallel, plan.chunks.size))
        val completionService: CompletionService<Unit> = ExecutorCompletionService(executor)

        try {
            val submittedTasks = plan.chunks.size

            for (chunk in plan.chunks) {
                completionService.submit(Callable {
                    val bytes = resourceGateway.downloadRange(url, chunk.range)
                    chunkStorage.save(chunk.index, bytes)
                    Unit
                })
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

    /**
     * Normalizes execution failures from worker tasks into adapter-specific runtime exception.
     *
     * Preserves interrupt status on [InterruptedException] and rethrows JVM [Error] causes.
     */
    private fun unwrapExecutionException(exception: Exception): RuntimeException {
        return when (exception) {
            is InterruptedException -> {
                Thread.currentThread().interrupt()
                AdapterException("Parallel download interrupted", exception)
            }

            is ExecutionException -> {
                val cause = exception.cause ?: exception
                if (cause is Error) {
                    throw cause
                }
                AdapterException("Parallel chunk download failed", cause)
            }

            else -> AdapterException("Parallel chunk download failed", exception)
        }
    }
}
