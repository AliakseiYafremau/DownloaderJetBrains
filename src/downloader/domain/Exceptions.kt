package downloader.domain


open class AppException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

open class BusinessLogicException(
    message: String,
    cause: Throwable? = null,
) : AppException(message, cause)

class InvalidDataException(
    message: String,
    cause: Throwable? = null,
) : BusinessLogicException(message, cause)

