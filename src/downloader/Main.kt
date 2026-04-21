package downloader

import downloader.adapters.DefaultParallelChunkDownloader
import downloader.adapters.FilesystemChunkStorage
import downloader.adapters.HttpResourceGateway
import downloader.application.usecase.DownloadFileUseCase
import downloader.cli.CliArgsParser
import downloader.domain.AppException
import downloader.domain.algorithms.DefaultChunkPlanner
import downloader.domain.algorithms.Timer
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val cliArgs = try {
        CliArgsParser.parse(args)
    } catch (exception: IllegalArgumentException) {
        System.err.println(exception.message)
        System.err.println()
        System.err.println(CliArgsParser.usage)
        exitProcess(1)
    }

    val useCase = DownloadFileUseCase(
        resourceGateway = HttpResourceGateway(),
        chunkPlanner = DefaultChunkPlanner(),
        chunkStorage = FilesystemChunkStorage(),
        parallelChunkDownloader = DefaultParallelChunkDownloader(),
        timer = Timer(),
    )

    try {
        val elapsedMillis = useCase.execute(
            url = cliArgs.url,
            targetPath = cliArgs.targetPath,
            config = cliArgs.config,
        )
        val elapsedSeconds = elapsedMillis / 1000.0

        println("Downloaded successfully to: ${cliArgs.targetPath}")
        println("Time spent: %.3f s".format(elapsedSeconds))
    } catch (exception: AppException) {
        System.err.println("Download failed: ${exception.message}")
        exitProcess(1)
    } catch (exception: Exception) {
        System.err.println("Unexpected error: ${exception.message}")
        exitProcess(1)
    }
}

