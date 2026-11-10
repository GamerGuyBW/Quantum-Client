package quantum.shared

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable

@Serializable
private data class GameVersionCacheFile(
	val game_versions: Map<String, String?> = emptyMap()
)

object LoaderGameVersionCache {
	fun load(loader: Loader): Map<String, String?> {
		val cache_file = cacheFile(loader)
		if(Files.notExists(cache_file)) {
			return emptyMap()
		}

		val file_text = Files.readString(cache_file)
		return QuantumJson.json.decodeFromString(GameVersionCacheFile.serializer(), file_text).game_versions
	}

	fun save(loader: Loader, game_versions: Map<String, String?>) {
		val file_text = QuantumJson.json.encodeToString(GameVersionCacheFile.serializer(), GameVersionCacheFile(game_versions))
		Files.writeString(cacheFile(loader), file_text)
	}

	private fun cacheFile(loader: Loader): Path {
		return QuantumPaths.cache_folder.resolve("${loader.name.lowercase()}_game_versions.json")
	}
}