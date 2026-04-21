package downloader.application.interfaces

import downloader.domain.ByteRange
import downloader.domain.ResourceMetadata

interface ResourceGateway {
    fun fetchMetadata(url: String): ResourceMetadata

    /**
     * Downloads specific range of data from the URL specified
     */
    fun downloadRange(url: String, range: ByteRange): ByteArray
}