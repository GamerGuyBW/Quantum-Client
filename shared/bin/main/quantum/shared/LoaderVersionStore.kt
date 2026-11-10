package quantum.shared

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.builtins.ListSerializer

object LoaderVersionStore {
	private const val fabric_loader_url = "https://meta.fabricmc.net/v2/versions/loader/"
	private const val fabric_game_url = "https://meta.fabricmc.net/v2/versions/game"
	private const val quilt_loader_url = "https://meta.quiltmc.org/v3/versions/loader/"
	private const val quilt_game_url = "https://meta.quiltmc.org/v3/versions/game"

	private val supported_game_versions_cache = ConcurrentHashMap<Loader, Set<String>>()

	fun hasVersionList(loader: Loader): Boolean {
		return loader == Loader.FABRIC || loader == Loader.QUILT
	}

	fun fetchSupportedGameVersions(loader: Loader): Set<String>? {
		val cached_versions = supported_game_versions_cache[loader]
		if(cached_versions != null) {
			return cached_versions
		}

		val supported_versions = when(loader) {
			Loader.FABRIC -> fetchGameVersionList(fabric_game_url)
			Loader.QUILT -> fetchGameVersionList(quilt_game_url)
			else -> return null
		}

		supported_game_versions_cache[loader] = supported_versions
		return supported_versions
	}

	fun fetchVersions(loader: Loader, game_version_id: String): List<LoaderVersion> {
		return when(loader) {
			Loader.FABRIC -> fetchFabricVersions(game_version_id)
			Loader.QUILT -> fetchQuiltVersions(game_version_id)
			else -> emptyList()
		}
	}

	private fun fetchGameVersionList(url: String): Set<String> {
		val file_text = QuantumHttp.fetchText(url)
		val entries = QuantumJson.json.decodeFromString(ListSerializer(LoaderGameVersion.serializer()), file_text)
		return entries.map { entry -> entry.version }.toSet()
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