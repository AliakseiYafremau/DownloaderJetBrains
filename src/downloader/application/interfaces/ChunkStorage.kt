package downloader.application.interfaces

/**
 * Responsible for temporary storage of downloaded chunks and final file assembly.
 *
 * Does NOT:
 * - make network requests
 * - plan chunks
 * - manage thread pools
 *
 * Does:
 * - accept a chunk and its bytes
 * - store so it can be assembled in correct order
 * - assemble final file
 * - clean up temporary data if needed
 */
interface ChunkStorage {
    /**
     * Save a chunk with its content.
     */
    fun save(chunkIndex: Int, bytes: ByteArray)

    /**
     * Assemble all stored chunks into the target file.
     * Chunks are written in order of their indices.
     */
    fun assemble(targetPath: String)

    /**
     * Clean up temporary storage.
     */
    fun cleanup()
}