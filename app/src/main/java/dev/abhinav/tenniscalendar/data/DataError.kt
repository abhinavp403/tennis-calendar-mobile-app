package dev.abhinav.tenniscalendar.data

/** Domain error type. Platform exceptions (IOException, HttpException, …) never leak past the repository. */
enum class DataError {
    /** No connectivity and no usable disk cache to fall back to. */
    NO_CONNECTION_NO_CACHE,

    /** Server returned a non-2xx status and no cache was available. */
    SERVER,

    /** Payload could not be parsed and no cache was available. */
    SERIALIZATION,

    UNKNOWN,
}

/** Repository result: either data (possibly served from the offline cache) or a domain error. */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T, val fromCache: Boolean) : DataResult<T>
    data class Failure(val error: DataError) : DataResult<Nothing>
}
