package quantum.shared

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object AddonVersionStore {
	private const val liteloader_versions_url = "https://dl.liteloader.com/versions/versions.json"
	private const val launchwrapper_main_class = "net.minecraft.launchwrapper.Launch"

	fun prepareAddon(addon: Addon) {
		val support = when(addon) {
			Addon.LITELOADER -> liteLoaderSupport()
		}

		saveSupport(addon, support)
	}

	fun loadSupport(addon: Addon): Map<Loader, List<String>>? {
		val file_text = QuantumCache.readText(supportFileName(addon)) ?: return null
		val support = QuantumJson.json.decodeFromString(ListSerializer(AddonSupport.serializer()), file_text)

		return support.associate { loader_support -> loader_support.loader to loader_support.game_versions }
	}

	fun fetchVersions(addon: Addon, game_version_id: String): List<LoaderVersion> {
		return when(addon) {
			Addon.LITELOADER -> fetchLiteLoaderVersions(game_version_id)
		}
	}

	fun needsBaseCheck(addon: Addon, loader: Loader): Boolean {
		return when(addon) {
			Addon.LITELOADER -> loader == Loader.FORGE
		}
	}

	fun verifyBase(addon: Addon, loader: Loader, loader_version: LoaderVersion?): String? {
		if(!needsBaseCheck(addon, loader) || loader_version == null) {
			return null
		}

		val installer_url = loader_version.installer_url ?: return null
		val main_class = InstallerInfo.launchMainClass(installer_url)

		if(main_class == launchwrapper_main_class) {
			return null
		}

		return "${addon.display_name} cannot run on ${loader.full_name} `${loader_version.display_name}`. Choose a different ${loader.full_name} version, or untick ${addon.display_name}."
	}

	private fun liteLoaderSupport(): Map<Loader, List<String>> {
		val game_versions = liteLoaderGameVersions().keys.toList()
		val forge_mapping = LoaderVersionStore.forgeVersionMapping()

		val forge_game_versions = LoaderVersionStore.readInParallel(game_versions) { game_version ->
			val newest_forge_version = forge_mapping[game_version]?.lastOrNull()
			val main_class = newest_forge_version?.let { forge_version -> InstallerInfo.launchMainClass(LoaderVersionStore.forgeInstallerUrl(forge_version)) }

			if(main_class == launchwrapper_main_class) game_version else null
		}.filterNotNull()

		return mapOf(
			Loader.VANILLA to game_versions,
			Loader.FORGE to forge_game_versions
		)
	}

	private fun liteLoaderGameVersions(): JsonObject {
		val versions_text = QuantumCache.fetchText(liteloader_versions_url)
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

	private fun saveSupport(addon: Addon, support: Map<Loader, List<String>>) {
		val release_order = VersionStore.fetchManifest().versions
			.sortedByDescending { version -> version.release_time }
			.mapIndexed { index, version -> version.id to index }
			.toMap()

		val support_entries = Loader.entries
			.filter { loader -> loader in support }
			.map { loader ->
				val sorted_versions = support.getValue(loader).sortedBy { game_version -> release_order[game_version] ?: Int.MAX_VALUE }
				AddonSupport(loader, sorted_versions)
			}

		val file_text = QuantumJson.json.encodeToString(ListSerializer(AddonSupport.serializer()), support_entries)
		QuantumCache.writeText(supportFileName(addon), file_text)
	}

	private fun supportFileName(addon: Addon): String {
		return "addon_support_${addon.name.lowercase()}.json"
	}
}