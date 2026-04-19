package downloader.application

import downloader.domain.ByteRange
import downloader.domain.ResourceMetadata


interface ChunkGateway {
    fun fetchMetadata(): ResourceMetadata
    fun downloadRange(range: ByteRange): ByteArray
}


