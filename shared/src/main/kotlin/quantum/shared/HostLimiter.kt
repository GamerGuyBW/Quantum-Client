package quantum.shared

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class RequestOutcome {
	NORMAL,
	NOT_TIMED,
	THROTTLED,
	FAILED
}

object HostLimiter {
	private const val starting_limit = 4
	private const val max_limit = 16
	private const val slow_factor = 3.0
	private const val slow_margin_seconds = 0.5
	private const val smoothing = 0.2

	private val host_limits = ConcurrentHashMap<String, HostLimit>()

	private class HostLimit {
		val lock = ReentrantLock()
		val changed: Condition = lock.newCondition()
		var limit = starting_limit
		var in_flight = 0
		var typical_seconds: Double? = null
		var calm_count = 0
		var paused_until_nanos = 0L
	}

	fun acquire(host: String) {
		val host_limit = host_limits.computeIfAbsent(host) { HostLimit() }

		host_limit.lock.withLock {
			while(true) {
				val pause_nanos = host_limit.paused_until_nanos - System.nanoTime()

				if(pause_nanos > 0) {
					host_limit.changed.awaitNanos(pause_nanos)
				} else if(host_limit.in_flight >= host_limit.limit) {
					host_limit.changed.await()
				} else {
					host_limit.in_flight += 1
					return
				}
			}
		}
	}

	fun release(host: String, duration_seconds: Double, outcome: RequestOutcome, retry_after_seconds: Long?) {
		val host_limit = host_limits.getValue(host)

		host_limit.lock.withLock {
			host_limit.in_flight -= 1

			when(outcome) {
				RequestOutcome.THROTTLED -> {
					host_limit.limit = maxOf(1, host_limit.limit / 2)
					host_limit.calm_count = 0
					host_limit.paused_until_nanos = System.nanoTime() + (retry_after_seconds ?: 0L) * 1_000_000_000L
				}

				RequestOutcome.FAILED -> {
					host_limit.limit = maxOf(1, host_limit.limit - 1)
					host_limit.calm_count = 0
				}

				RequestOutcome.NOT_TIMED -> {
				}

				RequestOutcome.NORMAL -> {
					val typical_seconds = host_limit.typical_seconds

					val is_slow = typical_seconds != null &&
						duration_seconds > typical_seconds * slow_factor &&
						duration_seconds > typical_seconds + slow_margin_seconds

					if(is_slow) {
						host_limit.limit = maxOf(1, host_limit.limit - 1)
						host_limit.calm_count = 0
					} else {
						host_limit.typical_seconds = if(typical_seconds == null) {
							duration_seconds
						} else {
							typical_seconds + (duration_seconds - typical_seconds) * smoothing
						}

						host_limit.calm_count += 1

						if(host_limit.calm_count >= host_limit.limit * 2 && host_limit.limit < max_limit) {
							host_limit.limit += 1
							host_limit.calm_count = 0
						}
					}
				}
			}

			host_limit.changed.signalAll()
		}
	}
}