package quantum.shared

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class HttpStatusException(val status_code: Int, url: String) : IOException("`$url` returned status `$status_code`.")

object QuantumHttp {
	private val http_client: HttpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build()

	fun fetchText(url: String): String {
		val request = HttpRequest.newBuilder(URI.create(url))
			.timeout(Duration.ofSeconds(20))
			.header("User-Agent", QuantumInfo.app_name)
			.GET()
			.build()

		val response = http_client.send(request, HttpResponse.BodyHandlers.ofString())

		if(response.statusCode() != 200) {
			throw HttpStatusException(response.statusCode(), url)
		}

		return response.body()
	}
}