package downloader.domain

open class BusinessLogicException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class InvalidDataException(
    message: String,
    cause: Throwable? = null,
) : BusinessLogicException(message, cause)

