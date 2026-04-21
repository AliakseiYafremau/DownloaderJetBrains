package downloader.adapters

import downloader.application.interfaces.ResourceGateway
import downloader.domain.ByteRange
import downloader.domain.ResourceMetadata
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * HTTP Gateway that retrieves data from HTTP/HTTPS resources
 */
class HttpResourceGateway(
    private val connectTimeout: Duration = Duration.ofSeconds(10),
    private val requestTimeout: Duration = Duration.ofSeconds(30),
) : ResourceGateway {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /**
     * Retrieves metadata about the downloadable resource (length, support for range reads).
     *
     * This is done by sending a HEAD request to the resource and parsing the response headers: `Content-Length` and `Accept-Ranges`.
     *
     * @return `ResourceMetadata` with info about the length and whether it supports range reads.
     * @throws AdapterException if the request fails, or if the response is missing required headers, or if the headers are invalid.
     */
    override fun fetchMetadata(url: String): ResourceMetadata {
        val uri = parseUri(url)

        val request = HttpRequest.newBuilder()
            .uri(uri)
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

    /**
     * Downloads an inclusive byte range from the resource URL.
     *
     * Expects HTTP 206 with a matching `Content-Range` header. HTTP 200 is accepted only
     * when requesting from byte 0, to tolerate servers that ignore the `Range` header and
     * return the full payload.
     *
     * @return  Byte array with the downloaded data for the requested range.
     * @throws AdapterException if the request fails
     */
    override fun downloadRange(url: String, range: ByteRange): ByteArray {
        val uri = parseUri(url)
        val rangeHeader = "bytes=${range.start}-${range.end}"

        val request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(requestTimeout)
            .header("Range", rangeHeader)
            .GET()
            .build()

        val response = execute(request, HttpResponse.BodyHandlers.ofByteArray())
        when (response.statusCode()) {
            206 -> validateContentRange(response, range)
            200 -> {
                if (range.start != 0L) {
                    throw AdapterException(
                        "Server ignored Range and returned 200 for non-full request $rangeHeader"
                    )
                }
            }

            else -> {
                throw AdapterException("Range request failed with status ${response.statusCode()} for $rangeHeader")
            }
        }

        val bytes = response.body()
        val expectedSize = range.end - range.start + 1
        if (bytes.size.toLong() != expectedSize) {
            throw AdapterException("Expected $expectedSize bytes for $rangeHeader, got ${bytes.size}")
        }

        return bytes
    }

    /**
     *  Parses the input URL string into a URI object, validating that it is a well-formed absolute URL with http or https scheme.
     *
     *  Checks if
     * - the URL is not blank
     * - the URL is absolute
     * - the URL scheme is http or https
     * - the URL has a non-blank host
     */
    private fun parseUri(url: String): URI {
        if (url.isBlank()) {
            throw AdapterException("URL must not be blank")
        }

        return try {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase()

            if (!uri.isAbsolute) {
                throw AdapterException("URL must be absolute")
            }
            if (scheme != "http" && scheme != "https") {
                throw AdapterException("URL scheme must be http or https")
            }
            if (uri.host.isNullOrBlank()) {
                throw AdapterException("URL host must not be blank")
            }

            uri
        } catch (exception: AdapterException) {
            throw exception
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

    /**
     * Validates that the Content-Range header in the response matches the requested byte range.
     *
     * Expects a header in the format: Content-Range: bytes start-end/total or bytes start-end
     *
     * @throws AdapterException if the header is missing, invalid, or does not match the requested range.
     */
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
