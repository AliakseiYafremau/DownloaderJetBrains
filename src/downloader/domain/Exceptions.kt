package downloader.domain

/**
 * App level exception. All exceptions in the app should extend this class.
 */
open class AppException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Business level exception. It represents an error that occurs during the execution of business logic.
 */
open class BusinessLogicException(
    message: String,
    cause: Throwable? = null,
) : AppException(message, cause)

class InvalidDataException(
    message: String,
    cause: Throwable? = null,
) : BusinessLogicException(message, cause)

