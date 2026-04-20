package downloader.domain

class BusinessLogicException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

