package downloader.adapters

import downloader.domain.AppException

/**
 * Adapter level exception.
 *
 * It represents an error that occurs during the execution of adapters of interfaces..
 */
open class AdapterException(
    message: String,
    cause: Throwable? = null,
) : AppException(message, cause)

