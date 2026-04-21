package downloader.application.interfaces

/**
 * Storage that manage chunk saving
 */
interface ChunkStorage {
    /**
     * Save a chunk with its content into temporary storage.
     */
    fun save(chunkIndex: Int, bytes: ByteArray)

    /**
     * Assemble all stored chunks from temporary storage into the target file.
     * Chunks are written in order of their indices.
     */
    fun assemble(targetPath: String)

    /**
     * Clean up temporary storage.
     */
    fun cleanup()
}