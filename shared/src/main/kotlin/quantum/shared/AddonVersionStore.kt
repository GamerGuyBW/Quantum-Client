package quantum.shared

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object AddonVersionStore {
	private const val liteloader_versions_url = "https://dl.liteloader.com/versions/versions.json"

	fun prepareAddon(addon: Addon) {
		when(addon) {
			Addon.LITELOADER -> saveSupportedGameVersions(addon, liteLoaderGameVersions().keys)
		}
	}

	fun loadSupportedGameVersions(addon: Addon): Set<String>? {
		val file_text = QuantumCache.readText(supportedFileName(addon)) ?: return null
		return QuantumJson.json.decodeFromString(ListSerializer(String.serializer()), file_text).toSet()
	}

	fun fetchVersions(addon: Addon, game_version_id: String): List<LoaderVersion> {
		return when(addon) {
			Addon.LITELOADER -> fetchLiteLoaderVersions(game_version_id)
		}
	}

	private fun liteLoaderGameVersions(): JsonObject {
		val versions_text = QuantumCache.textOrFetch("liteloader_versions.json", liteloader_versions_url)
		return QuantumJson.json.parseToJsonElement(versions_text).jsonObject["versions"]?.jsonObject ?: JsonObject(emptyMap())
	}

	private fun fetchLiteLoaderVersions(game_version_id: String): List<LoaderVersion> {
		val game_details = liteLoaderGameVersions()[game_version_id]?.jsonObject ?: return emptyList()

		val builds = listOf("artefacts", "snapshots").flatMap { section_name ->
			val section_builds = game_details[section_name]?.jsonObject?.get("com.mumfrey:liteloader")?.jsonObject

			if(section_builds == null) {
				emptyList()
			} else {
				section_builds
					.filterKeys { build_key -> build_key != "latest" }
					.values
					.map { build_element -> QuantumJson.json.decodeFromJsonElement(LiteLoaderBuild.serializer(), build_element) }
			}
		}

		return builds
			.distinctBy { build -> build.md5 }
			.sortedByDescending { build -> build.timestamp.toLongOrNull() ?: 0L }
			.map { build ->
				val addon_version = build.build ?: build.version

				LoaderVersion(
					version = addon_version,
					display_name = addon_version.removePrefix("$game_version_id-"),
					is_stable = build.stream == "RELEASE"
				)
			}
	}

	private fun saveSupportedGameVersions(addon: Addon, supported_versions: Set<String>) {
		val file_text = QuantumJson.json.encodeToString(ListSerializer(String.serializer()), supported_versions.toList())
		QuantumCache.writeText(supportedFileName(addon), file_text)
	}

	private fun supportedFileName(addon: Addon): String {
		return "supported_game_versions_addon_${addon.name.lowercase()}.json"
	}
}