package quantum.shared

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.builtins.ListSerializer

object LoaderVersionStore {
	private const val fabric_loader_url = "https://meta.fabricmc.net/v2/versions/loader/"
	private const val quilt_loader_url = "https://meta.quiltmc.org/v3/versions/loader/"

	fun hasVersionList(loader: Loader): Boolean {
		return loader == Loader.FABRIC || loader == Loader.QUILT
	}

	fun fetchVersions(loader: Loader, game_version_id: String): List<LoaderVersion> {
		return when(loader) {
			Loader.FABRIC -> fetchFabricVersions(game_version_id)
			Loader.QUILT -> fetchQuiltVersions(game_version_id)
			else -> emptyList()
		}
	}

	private fun fetchFabricVersions(game_version_id: String): List<LoaderVersion> {
		val file_text = fetchUnlessUnsupported(fabric_loader_url + encodeForPath(game_version_id))
		if(file_text == null) {
			return emptyList()
		}

		val entries = QuantumJson.json.decodeFromString(ListSerializer(FabricLoaderEntry.serializer()), file_text)
		return entries.map { entry -> LoaderVersion(entry.loader.version, entry.loader.stable) }
	}

	private fun fetchQuiltVersions(game_version_id: String): List<LoaderVersion> {
		val file_text = fetchUnlessUnsupported(quilt_loader_url + encodeForPath(game_version_id))
		if(file_text == null) {
			return emptyList()
		}

		val entries = QuantumJson.json.decodeFromString(ListSerializer(QuiltLoaderEntry.serializer()), file_text)
		return entries.map { entry -> LoaderVersion(entry.loader.version, !entry.loader.version.contains("-")) }
	}

	private fun fetchUnlessUnsupported(url: String): String? {
		return try {
			QuantumHttp.fetchText(url)
		} catch(error: HttpStatusException) {
			if(error.status_code == 400 || error.status_code == 404) {
				null
			} else {
				throw error
			}
		}
	}

	private fun encodeForPath(game_version_id: String): String {
		return URLEncoder.encode(game_version_id, StandardCharsets.UTF_8).replace("+", "%20")
	}
}