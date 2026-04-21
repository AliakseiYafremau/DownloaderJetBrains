package downloader

import downloader.adapters.DefaultParallelChunkDownloader
import downloader.adapters.FilesystemChunkStorage
import downloader.adapters.HttpResourceGateway
import downloader.application.usecase.DownloadFileUseCase
import downloader.domain.AppException
import downloader.domain.DownloadConfig
import downloader.domain.algorithms.DefaultChunkPlanner
import kotlin.system.exitProcess

private const val EXPECTED_ARGS_COUNT = 5

private val USAGE = """
Usage:
  downloader.MainKt <url> <targetPath> <minChunkSize> <maxChunkSize> <maxParallelDownloads>

Example:
  downloader.MainKt http://localhost:8080/file.bin /tmp/file.bin 1048576 8388608 4
""".trimIndent()

fun main(args: Array<String>) {
    if (args.size != EXPECTED_ARGS_COUNT) {
        printUsage("Expected $EXPECTED_ARGS_COUNT arguments, got ${args.size}.")
    }

    val url = args[0]
    val targetPath = args[1]
    val minChunkSize = parseLongArg(args[2], "minChunkSize")
    val maxChunkSize = parseLongArg(args[3], "maxChunkSize")
    val maxParallelDownloads = parseMaxParallelArg(args[4])

    val useCase = DownloadFileUseCase(
        resourceGateway = HttpResourceGateway(),
        chunkPlanner = DefaultChunkPlanner(),
        chunkStorage = FilesystemChunkStorage(),
        parallelChunkDownloader = DefaultParallelChunkDownloader(),
    )

    try {
        val config = DownloadConfig(
            minChunkSize = minChunkSize,
            maxChunkSize = maxChunkSize,
            maxParallelDownloads = maxParallelDownloads,
        )

        useCase.execute(
            url = url,
            targetPath = targetPath,
            config = config,
        )

        println("Downloaded successfully to: $targetPath")
    } catch (exception: AppException) {
        System.err.println("Download failed: ${exception.message}")
        exitProcess(1)
    } catch (exception: Exception) {
        System.err.println("Unexpected error: ${exception.message}")
        exitProcess(1)
    }
}

private fun parseLongArg(value: String, argName: String): Long {
    val parsed = value.toLongOrNull()
    if (parsed == null) {
        printUsage("Invalid number for $argName: $value")
    }

    return parsed
}

private fun parseMaxParallelArg(value: String): Int {
    val parsed = value.toIntOrNull()
    if (parsed == null) {
        printUsage("Invalid number for maxParallelDownloads: $value")
    }

    return parsed
}

private fun printUsage(message: String): Nothing {
    System.err.println(message)
    System.err.println()
    System.err.println(USAGE)
    exitProcess(1)
}


