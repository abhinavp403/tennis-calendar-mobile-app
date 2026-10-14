package dev.abhinav.tenniscalendar.data

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching] for suspending work, but lets [CancellationException] through so a cancelled
 * coroutine (e.g. a stopped WorkManager worker) actually stops instead of carrying on as if a
 * best-effort step had merely failed.
 */
inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
