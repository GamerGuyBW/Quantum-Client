package quantum.shared

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object VersionStore {
	private const val manifest_url = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

	private val http_client: HttpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build()

	fun fetchManifest(): VersionManifest {
		val request = HttpRequest.newBuilder(URI.create(manifest_url))
			.timeout(Duration.ofSeconds(20))
			.header("User-Agent", QuantumInfo.app_name)
			.GET()
			.build()

		val response = http_client.send(request, HttpResponse.BodyHandlers.ofString())

		if(response.statusCode() != 200) {
			throw IOException("Mojang returned status `${response.statusCode()}`.")
		}

		return QuantumJson.json.decodeFromString(VersionManifest.serializer(), response.body())
	}
}