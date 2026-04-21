package downloader.cli

import downloader.domain.DownloadConfig

data class ParsedCliArgs(
    val url: String,
    val targetPath: String,
    val config: DownloadConfig,
)

object CliArgsParser {
    private const val MIN_ARGS_COUNT = 2
    private const val MAX_ARGS_COUNT = 5

    val usage: String = """
Usage:
  downloader.MainKt <url> <targetPath> [minChunkSize] [maxChunkSize] [maxParallelDownloads]

Example:
  downloader.MainKt http://localhost:8080/file.bin /tmp/file.bin
  downloader.MainKt http://localhost:8080/file.bin /tmp/file.bin 1048576 8388608 4
  downloader.MainKt http://localhost:8080/file.bin /tmp/file.bin 1048576 null 8
""".trimIndent()

    fun parse(args: Array<String>): ParsedCliArgs {
        if (args.size !in MIN_ARGS_COUNT..MAX_ARGS_COUNT) {
            throw IllegalArgumentException(
                "Expected from $MIN_ARGS_COUNT to $MAX_ARGS_COUNT arguments, got ${args.size}."
            )
        }

        return ParsedCliArgs(
            url = args[0],
            targetPath = args[1],
            config = parseConfig(args),
        )
    }

    private fun parseConfig(args: Array<String>): DownloadConfig {
        return when (args.size) {
            2 -> DownloadConfig()
            3 -> DownloadConfig(minChunkSize = parseLongArg(args[2], "minChunkSize"))
            4 -> DownloadConfig(
                minChunkSize = parseLongArg(args[2], "minChunkSize"),
                maxChunkSize = parseNullableLongArg(args[3], "maxChunkSize"),
            )

            5 -> DownloadConfig(
                minChunkSize = parseLongArg(args[2], "minChunkSize"),
                maxChunkSize = parseNullableLongArg(args[3], "maxChunkSize"),
                maxParallelDownloads = parseIntArg(args[4], "maxParallelDownloads"),
            )

            else -> throw IllegalArgumentException("Invalid number of arguments: ${args.size}")
        }
    }

    private fun parseLongArg(value: String, argName: String): Long {
        return value.toLongOrNull()
            ?: throw IllegalArgumentException("Invalid number for $argName: $value")
    }

    private fun parseNullableLongArg(value: String, argName: String): Long? {
        if (value.equals("null", ignoreCase = true) || value == "-") {
            return null
        }

        return parseLongArg(value, argName)
    }

    private fun parseIntArg(value: String, argName: String): Int {
        return value.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid number for $argName: $value")
    }
}

