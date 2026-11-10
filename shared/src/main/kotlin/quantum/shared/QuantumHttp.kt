package quantum.shared

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class HttpStatusException(val status_code: Int, url: String) : IOException("`$url` returned status `$status_code`.")

class HttpBytes(val status_code: Int, val body: ByteArray, val total_size: Long?)

object QuantumHttp {
	private const val max_attempts = 5
	private const val default_retry_after_seconds = 5L

	private val http_client: HttpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build()

	fun fetchText(url: String): String {
		val request = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(30))
			.header("User-Agent", QuantumInfo.app_name)
			.GET()
			.build()

		val response = send(request, HttpResponse.BodyHandlers.ofString())

		if(response.statusCode() != 200) {
			throw HttpStatusException(response.statusCode(), url)
		}

		return response.body()
	}

	fun fetchBytes(url: String, range: String? = null): HttpBytes {
		val request_builder = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(60))
			.header("User-Agent", QuantumInfo.app_name)
			.GET()

		if(range != null) {
			request_builder.header("Range", range)
		}

		val response = send(request_builder.build(), HttpResponse.BodyHandlers.ofByteArray())

		if(response.statusCode() != 200 && response.statusCode() != 206) {
			throw HttpStatusException(response.statusCode(), url)
		}

		val content_range = response.headers().firstValue("Content-Range").orElse(null)
		val total_size = content_range?.substringAfter("/")?.toLongOrNull()

		return HttpBytes(response.statusCode(), response.body(), total_size)
	}

	private fun <T> send(request: HttpRequest, body_handler: HttpResponse.BodyHandler<T>): HttpResponse<T> {
		val host = request.uri().host

		repeat(max_attempts) {
			HostLimiter.acquire(host)

			val start_nanos = System.nanoTime()
			var outcome = RequestOutcome.FAILED
			var retry_after_seconds: Long? = null

			try {
				val response = http_client.send(request, body_handler)
				val status_code = response.statusCode()

				if(status_code == 429 || status_code == 503) {
					val retry_after_header = response.headers().firstValue("Retry-After").orElse(null)
					retry_after_seconds = retry_after_header?.toLongOrNull() ?: default_retry_after_seconds
					outcome = RequestOutcome.THROTTLED
				} else {
					outcome = if(status_code in 200..299) RequestOutcome.NORMAL else RequestOutcome.NOT_TIMED
					return response
				}
			} finally {
				HostLimiter.release(host, (System.nanoTime() - start_nanos) / 1_000_000_000.0, outcome, retry_after_seconds)
			}
		}

		throw HttpStatusException(429, request.uri().toString())
	}
}