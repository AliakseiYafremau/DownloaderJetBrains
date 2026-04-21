package downloader.cli

import downloader.domain.InvalidDataException
import java.net.URI
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

object TargetPathResolver {
    private const val DEFAULT_FILE_NAME = "downloaded.file"

    fun resolve(url: String, targetPath: String): String {
        val target = parseTargetPath(targetPath)
        if (!isDirectoryTarget(targetPath, target)) {
            return target.toString()
        }

        val fileName = extractFileNameFromUrl(url)
        return target.resolve(fileName).toString()
    }

    private fun parseTargetPath(targetPath: String): Path {
        if (targetPath.isBlank()) {
            throw InvalidDataException("targetPath must not be blank")
        }

        return try {
            Path.of(targetPath)
        } catch (exception: InvalidPathException) {
            throw InvalidDataException("Invalid targetPath: $targetPath", exception)
        }
    }

    private fun isDirectoryTarget(rawTargetPath: String, targetPath: Path): Boolean {
        return rawTargetPath.endsWith("/") || rawTargetPath.endsWith("\\") || Files.isDirectory(targetPath)
    }

    private fun extractFileNameFromUrl(url: String): String {
        val path = try {
            URI(url).path.orEmpty()
        } catch (_: Exception) {
            ""
        }

        val candidate = path.substringAfterLast('/').trim()
        return if (candidate.isNotEmpty()) {
            candidate
        } else {
            DEFAULT_FILE_NAME
        }
    }
}
