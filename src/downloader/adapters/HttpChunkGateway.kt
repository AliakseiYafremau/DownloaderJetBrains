package downloader.adapters

import downloader.application.interfaces.ChunkGateway
import downloader.domain.ByteRange
import downloader.domain.ResourceMetadata
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class HttpChunkGateway(
    private val connectTimeout: Duration = Duration.ofSeconds(10),
    private val requestTimeout: Duration = Duration.ofSeconds(30),
) : ChunkGateway {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    override fun fetchMetadata(url: String): ResourceMetadata {
        val request = HttpRequest.newBuilder()
            .uri(parseUri(url))
            .timeout(requestTimeout)
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .build()

        val response = execute(request, HttpResponse.BodyHandlers.discarding())
        if (response.statusCode() !in 200..299) {
            throw AdapterException("HEAD request failed with status ${response.statusCode()}")
        }

        val contentLength = response.headers()
            .firstValue("Content-Length")
            .orElseThrow { AdapterException("Missing Content-Length header") }
            .toLongOrNull()
            ?: throw AdapterException("Invalid Content-Length header")

        if (contentLength < 0) {
            throw AdapterException("Content-Length must be >= 0")
        }

        val supportsRangeReads = response.headers()
            .allValues("Accept-Ranges")
            .flatMap { it.split(',') }
            .map { it.trim().lowercase() }
            .any { it == "bytes" }

        return ResourceMetadata(
            supportsRangeReads = supportsRangeReads,
            contentLength = contentLength,
        )
    }

    override fun downloadRange(url: String, range: ByteRange): ByteArray {
        val rangeHeader = "bytes=${range.start}-${range.end}"

        val request = HttpRequest.newBuilder()
            .uri(parseUri(url))
            .timeout(requestTimeout)
            .header("Range", rangeHeader)
            .GET()
            .build()

        val response = execute(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() != 206) {
            throw AdapterException("Range request failed with status ${response.statusCode()} for $rangeHeader")
        }

        validateContentRange(response, range)

        val bytes = response.body()
        val expectedSize = range.end - range.start + 1
        if (bytes.size.toLong() != expectedSize) {
            throw AdapterException("Expected $expectedSize bytes for $rangeHeader, got ${bytes.size}")
        }

        return bytes
    }

    private fun parseUri(url: String): URI {
        return try {
            URI(url)
        } catch (exception: Exception) {
            throw AdapterException("Invalid URL: $url", exception)
        }
    }

    private fun <T> execute(request: HttpRequest, bodyHandler: HttpResponse.BodyHandler<T>): HttpResponse<T> {
        return try {
            client.send(request, bodyHandler)
        } catch (exception: Exception) {
            throw AdapterException("HTTP request failed: ${request.uri()}", exception)
        }
    }

    private fun validateContentRange(response: HttpResponse<*>, requestedRange: ByteRange) {
        val contentRange = response.headers().firstValue("Content-Range")
            .orElseThrow { AdapterException("Missing Content-Range header for partial response") }

        val parsed = CONTENT_RANGE_PATTERN.matchEntire(contentRange)
            ?: throw AdapterException("Invalid Content-Range header: $contentRange")

        val start = parsed.groupValues[1].toLong()
        val end = parsed.groupValues[2].toLong()

        if (start != requestedRange.start || end != requestedRange.end) {
            throw AdapterException(
                "Content-Range mismatch. Requested ${requestedRange.start}-${requestedRange.end}, got $start-$end"
            )
        }
    }

    private companion object {
        val CONTENT_RANGE_PATTERN = Regex("bytes\\s+(\\d+)-(\\d+)/(\\d+|\\*)")
    }
}

