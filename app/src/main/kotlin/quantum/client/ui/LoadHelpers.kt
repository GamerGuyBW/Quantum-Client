package quantum.client.ui

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

suspend fun <T> loadInterruptibly(load: () -> T): Result<T> {
	return try {
		Result.success(runInterruptible(Dispatchers.IO) { load() })
	} catch(error: CancellationException) {
		throw error
	} catch(error: Exception) {
		Result.failure(error)
	}
}