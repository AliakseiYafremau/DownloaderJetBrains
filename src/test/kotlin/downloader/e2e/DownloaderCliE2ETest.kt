package downloader.e2e

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("e2e")
class DownloaderCliE2ETest {

    @Test
    fun `downloads large file via cli from local range server`(@TempDir tempDir: Path) {
        // Arrange
        val targetFile = tempDir.resolve("downloaded.bin")
        val sourceSizeBytes = configuredLargeFileSizeBytes()
        val expectedHash = deterministicSha256(sourceSizeBytes)

        LocalRangeFileServer(sourceSizeBytes).use { server ->
            val url = "${server.baseUrl}/generated.bin"

            // Act
            val cliResult = runCliDownloader(url, targetFile)

            // Assert
            assertTrue(cliResult.completed, "Downloader CLI timed out. Output:\n${cliResult.output}")
            assertEquals(0, cliResult.exitCode, "Downloader CLI failed. Output:\n${cliResult.output}")
            assertTrue(Files.exists(targetFile), "Target file was not created")
            assertEquals(sourceSizeBytes, Files.size(targetFile), "Downloaded file size mismatch")
            assertEquals(expectedHash, sha256(targetFile), "Downloaded file hash mismatch")
        }
    }

    private fun runCliDownloader(url: String, targetFile: Path): CliRunResult {
        val javaTmpDir = System.getProperty("java.io.tmpdir")
        val process = ProcessBuilder(
            javaExecutablePath(),
            "-Djava.io.tmpdir=$javaTmpDir",
            "-cp",
            System.getProperty("java.class.path"),
            "downloader.MainKt",
            url,
            targetFile.toString(),
            MIN_CHUNK_SIZE_BYTES.toString(),
            MAX_CHUNK_SIZE_BYTES.toString(),
            MAX_PARALLEL_DOWNLOADS.toString(),
        )
            .redirectErrorStream(true)
            .start()

        val completed = process.waitFor(CLI_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        if (!completed) {
            process.destroyForcibly()
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = if (completed) process.exitValue() else -1
        return CliRunResult(completed = completed, exitCode = exitCode, output = output)
    }

    private fun javaExecutablePath(): String {
        val javaBinary = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            "java.exe"
        } else {
            "java"
        }
        return Path.of(System.getProperty("java.home"), "bin", javaBinary).toString()
    }

    private fun configuredLargeFileSizeBytes(): Long {
        val configuredMb = System.getProperty(E2E_FILE_SIZE_MB_PROPERTY, DEFAULT_E2E_FILE_SIZE_MB.toString())
        val sizeMb = configuredMb.toLongOrNull()
            ?: throw IllegalArgumentException(
                "Invalid system property $E2E_FILE_SIZE_MB_PROPERTY=$configuredMb. Expected a positive integer."
            )
        if (sizeMb <= 0) {
            throw IllegalArgumentException(
                "System property $E2E_FILE_SIZE_MB_PROPERTY must be > 0, got $sizeMb."
            )
        }
        return sizeMb * BYTES_IN_MB
    }

    private fun deterministicSha256(sizeBytes: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var offset = 0L
        var remaining = sizeBytes
        while (remaining > 0) {
            val bytesToFill = minOf(buffer.size.toLong(), remaining).toInt()
            fillDeterministicBytes(buffer, offset, bytesToFill)
            digest.update(buffer, 0, bytesToFill)
            offset += bytesToFill.toLong()
            remaining -= bytesToFill.toLong()
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }
    }

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }
    }

    private class LocalRangeFileServer(private val fileSizeBytes: Long) : AutoCloseable {
        private val executor = Executors.newFixedThreadPool(8)
        private val server: HttpServer = HttpServer.create(
            InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
            0
        )

        init {
            server.executor = executor
            server.createContext("/generated.bin") { exchange -> handleRequest(exchange) }
            server.start()
        }

        val baseUrl: String
            get() = "http://127.0.0.1:${server.address.port}"

        override fun close() {
            server.stop(0)
            executor.shutdownNow()
        }

        private fun handleRequest(exchange: HttpExchange) {
            try {
                when (exchange.requestMethod.uppercase()) {
                    "HEAD" -> handleHeadRequest(exchange)
                    "GET" -> handleGetRequest(exchange)
                    else -> exchange.sendResponseHeaders(405, -1)
                }
            } finally {
                exchange.close()
            }
        }

        private fun handleHeadRequest(exchange: HttpExchange) {
            exchange.responseHeaders.add("Accept-Ranges", "bytes")
            exchange.responseHeaders.add("Content-Length", fileSizeBytes.toString())
            exchange.sendResponseHeaders(200, -1)
        }

        private fun handleGetRequest(exchange: HttpExchange) {
            val rangeHeader = exchange.requestHeaders.getFirst("Range")
            if (rangeHeader == null) {
                sendFullContent(exchange)
                return
            }

            val match = RANGE_HEADER_PATTERN.matchEntire(rangeHeader.trim())
            val start = match?.groupValues?.get(1)?.toLongOrNull()
            val end = match?.groupValues?.get(2)?.toLongOrNull()
            if (start == null || end == null || start < 0 || end < start || end >= fileSizeBytes) {
                sendRangeNotSatisfiable(exchange)
                return
            }

            val chunkLength = end - start + 1
            exchange.responseHeaders.add("Accept-Ranges", "bytes")
            exchange.responseHeaders.add("Content-Length", chunkLength.toString())
            exchange.responseHeaders.add("Content-Range", "bytes $start-$end/$fileSizeBytes")
            exchange.sendResponseHeaders(206, chunkLength)
            writeRangeToResponse(start, chunkLength, exchange)
        }

        private fun sendFullContent(exchange: HttpExchange) {
            exchange.responseHeaders.add("Accept-Ranges", "bytes")
            exchange.responseHeaders.add("Content-Length", fileSizeBytes.toString())
            exchange.sendResponseHeaders(200, fileSizeBytes)
            exchange.responseBody.use { output ->
                writeDeterministicBytes(output, startOffset = 0L, length = fileSizeBytes)
            }
        }

        private fun sendRangeNotSatisfiable(exchange: HttpExchange) {
            exchange.responseHeaders.add("Content-Range", "bytes */$fileSizeBytes")
            exchange.sendResponseHeaders(416, -1)
        }

        private fun writeRangeToResponse(start: Long, length: Long, exchange: HttpExchange) {
            exchange.responseBody.use { output ->
                writeDeterministicBytes(output, startOffset = start, length = length)
            }
        }

        private fun writeDeterministicBytes(output: OutputStream, startOffset: Long, length: Long) {
            val buffer = ByteArray(8192)
            var offset = startOffset
            var remaining = length
            while (remaining > 0) {
                val bytesToWrite = minOf(buffer.size.toLong(), remaining).toInt()
                fillDeterministicBytes(buffer, offset, bytesToWrite)
                output.write(buffer, 0, bytesToWrite)
                offset += bytesToWrite.toLong()
                remaining -= bytesToWrite.toLong()
            }
        }
    }

    private data class CliRunResult(
        val completed: Boolean,
        val exitCode: Int,
        val output: String,
    )

    private companion object {
        private const val E2E_FILE_SIZE_MB_PROPERTY = "e2e.file.size.mb"
        private const val DEFAULT_E2E_FILE_SIZE_MB = 50L
        private const val BYTES_IN_MB = 1024L * 1024L
        private const val MIN_CHUNK_SIZE_BYTES = 1L * 1024 * 1024
        private const val MAX_CHUNK_SIZE_BYTES = 8L * 1024 * 1024
        private const val MAX_PARALLEL_DOWNLOADS = 4
        private const val CLI_TIMEOUT_MINUTES = 5L
        private val RANGE_HEADER_PATTERN = Regex("bytes=(\\d+)-(\\d+)")

        private fun fillDeterministicBytes(buffer: ByteArray, startOffset: Long, length: Int) {
            for (i in 0 until length) {
                buffer[i] = ((startOffset + i.toLong()) and 0xFF).toByte()
            }
        }
    }
}
