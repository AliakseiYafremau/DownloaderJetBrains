package downloader.adapters

import downloader.domain.AppException

open class AdapterException(
    message: String,
    cause: Throwable? = null,
) : AppException(message, cause)

