package downloader.application.interfaces

interface ChunkStorage {

    fun save(chunkIndex: Int, bytes: ByteArray)
    fun assemble(targetPath: String)
}