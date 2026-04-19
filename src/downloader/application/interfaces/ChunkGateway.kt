package downloader.application.interfaces

import downloader.domain.ByteRange
import downloader.domain.ResourceMetadata

interface ChunkGateway {
    fun fetchMetadata(url: String): ResourceMetadata
    fun downloadRange(url: String, range: ByteRange): ByteArray
}