package quantum.shared

import java.io.StringReader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.xml.sax.InputSource

object LoaderVersionStore {
	private const val fabric_meta_url = "https://meta.fabricmc.net/v2/versions/"
	private const val legacy_fabric_meta_url = "https://meta.legacyfabric.net/v2/versions/"
	private const val babric_meta_url = "https://meta.babric.glass-launcher.net/v2/versions/"
	private const val quilt_meta_url = "https://meta.quiltmc.org/v3/versions/"
	private const val neoforge_maven_url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/"
	private const val legacy_neoforge_maven_url = "https://maven.neoforged.net/releases/net/neoforged/forge/"
	private const val forge_maven_url = "https://maven.minecraftforge.net/releases/net/minecraftforge/forge/"
	private const val forge_mapping_url = "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json"
	private const val ornithe_meta_url = "https://meta.ornithemc.net/v3/versions/"
	private const val ornithe_profile_suffix = "-vanilla"
	private const val installer_read_threads = 16

	fun hasVersionList(loader: Loader): Boolean {
		return loader != Loader.VANILLA
	}

	fun prepareLoader(loader: Loader) {
		when(loader) {
			Loader.FABRIC -> saveSupportedGameVersions(loader, fetchGameVersionList(fabric_meta_url))
			Loader.LEGACY_FABRIC -> saveSupportedGameVersions(loader, fetchGameVersionList(legacy_fabric_meta_url))
			Loader.BABRIC -> saveSupportedGameVersions(loader, fetchGameVersionList(babric_meta_url))
			Loader.QUILT -> saveSupportedGameVersions(loader, fetchGameVersionList(quilt_meta_url))
			Loader.FORGE -> saveResolvedVersions(loader, resolveForgeVersions())
			Loader.NEOFORGE -> saveResolvedVersions(loader, resolveNeoForgeVersions())
			Loader.ORNITHE_FABRIC, Loader.ORNITHE_QUILT -> saveSupportedGameVersions(loader, ornitheGameVersions().values.flatten().toSet())
			Loader.VANILLA -> throw IllegalArgumentException("`${loader.display_name}` does not have a version list.")
		}
	}

	fun loadSupportedGameVersions(loader: Loader): Set<String>? {
		val file_text = QuantumCache.readText(supportedFileName(loader)) ?: return null
		return QuantumJson.json.decodeFromString(ListSerializer(String.serializer()), file_text).toSet()
	}

	fun hasGenerations(loader: Loader): Boolean {
		return loader == Loader.ORNITHE_FABRIC || loader == Loader.ORNITHE_QUILT
	}

	fun loadGenerations(loader: Loader, game_version_id: String): LoaderGenerations? {
		if(!hasGenerations(loader)) {
			return null
		}

		val available_generations = ornitheGameVersions()
			.filterValues { game_versions -> game_version_id in game_versions }
			.keys
			.sorted()

		return LoaderGenerations(available_generations, ornitheGenerations().stable_generation)
	}

	fun fetchVersions(loader: Loader, game_version_id: String, generation: Int? = null): List<LoaderVersion> {
		return when(loader) {
			Loader.FABRIC -> fetchFabricStyleVersions(loader, fabric_meta_url, game_version_id)
			Loader.LEGACY_FABRIC -> fetchFabricStyleVersions(loader, legacy_fabric_meta_url, game_version_id)
			Loader.BABRIC -> fetchFabricStyleVersions(loader, babric_meta_url, game_version_id)
			Loader.QUILT -> fetchQuiltVersions(game_version_id)
			Loader.FORGE, Loader.NEOFORGE -> fetchResolvedVersions(loader, game_version_id)
			Loader.ORNITHE_FABRIC, Loader.ORNITHE_QUILT -> fetchOrnitheVersions(loader, game_version_id, generation ?: throw IllegalArgumentException("`${loader.full_name}` needs a generation."))
			Loader.VANILLA -> emptyList()
		}
	}

	fun verifyGameVersion(loader: Loader, loader_version: LoaderVersion, expected_game_version: String): String? {
		val actual_game_version = if(loader_version.profile_url != null) {
			profileGameVersion(loader_version.profile_url)?.let { profile_version ->
				if(hasGenerations(loader)) profile_version.removeSuffix(ornithe_profile_suffix) else profile_version
			}
		} else if(loader_version.installer_url != null) {
			InstallerInfo.minecraftVersion(loader_version.installer_url) ?: forgeMappedGameVersion(loader, loader_version.version)
		} else {
			return null
		}

		if(actual_game_version == expected_game_version) {
			return null
		}

		return if(actual_game_version == null) {
			"${loader.full_name} `${loader_version.display_name}` is no longer available. Choose a different version."
		} else {
			"${loader.full_name} `${loader_version.display_name}` is for Minecraft `$actual_game_version`, not `$expected_game_version`. Choose a different version."
		}
	}

	private fun profileGameVersion(profile_url: String): String? {
		val profile_text = try {
			QuantumCache.fetchText(profile_url)
		} catch(error: HttpStatusException) {
			if(error.status_code == 400 || error.status_code == 404) {
				return null
			}
			throw error
		}

		return QuantumJson.json.parseToJsonElement(profile_text).jsonObject["inheritsFrom"]?.jsonPrimitive?.contentOrNull
	}

	private fun resolveNeoForgeVersions(): List<ResolvedLoaderVersion> {
		val mojang_versions = mojangVersionIds()
		val artifacts = mavenArtifactList(legacy_neoforge_maven_url, "forge") + mavenArtifactList(neoforge_maven_url, "neoforge")
		val rule_versions = artifacts.associate { artifact -> artifact.version to NeoForgeVersions.gameVersion(artifact.version) }

		val unplaced_artifacts = artifacts.filter { artifact -> rule_versions[artifact.version] !in mojang_versions }
		val installer_versions = readInParallel(unplaced_artifacts) { artifact ->
			artifact.version to InstallerInfo.minecraftVersion(artifact.installer_url)
		}.toMap()

		return artifacts.map { artifact ->
			val game_version = if(artifact.version in installer_versions) installer_versions[artifact.version] else rule_versions[artifact.version]
			ResolvedLoaderVersion(artifact.version, game_version, artifact.installer_url)
		}
	}

	private fun resolveForgeVersions(): List<ResolvedLoaderVersion> {
		val mojang_versions = mojangVersionIds()
		val mapped_versions = forgeVersionMapping().flatMap { (game_version, forge_versions) ->
			forge_versions.map { forge_version -> forge_version to game_version }
		}

		val unplaced_versions = mapped_versions.filter { (_, game_version) -> game_version !in mojang_versions }
		val installer_versions = readInParallel(unplaced_versions) { (forge_version, _) ->
			forge_version to InstallerInfo.minecraftVersion(forgeInstallerUrl(forge_version))
		}.toMap()

		return mapped_versions.map { (forge_version, mapped_game_version) ->
			val game_version = installer_versions[forge_version] ?: mapped_game_version
			ResolvedLoaderVersion(forge_version, game_version, forgeInstallerUrl(forge_version))
		}
	}

	internal fun <T, R> readInParallel(items: List<T>, read: (T) -> R): List<R> {
		if(items.isEmpty()) {
			return emptyList()
		}

		val executor = Executors.newFixedThreadPool(minOf(items.size, installer_read_threads))

		try {
			val tasks = items.map { item -> Callable { read(item) } }
			return executor.invokeAll(tasks).map { future -> future.get() }
		} finally {
			executor.shutdownNow()
		}
	}

	private fun saveResolvedVersions(loader: Loader, resolved_versions: List<ResolvedLoaderVersion>) {
		val resolved_text = QuantumJson.json.encodeToString(ListSerializer(ResolvedLoaderVersion.serializer()), resolved_versions)
		QuantumCache.writeText(resolvedFileName(loader), resolved_text)
		saveSupportedGameVersions(loader, resolved_versions.mapNotNull { resolved_version -> resolved_version.game_version }.toSet())
	}

	private fun loadResolvedVersions(loader: Loader): List<ResolvedLoaderVersion> {
		val resolved_text = QuantumCache.readText(resolvedFileName(loader)) ?: return emptyList()
		return QuantumJson.json.decodeFromString(ListSerializer(ResolvedLoaderVersion.serializer()), resolved_text)
	}

	private fun fetchResolvedVersions(loader: Loader, game_version_id: String): List<LoaderVersion> {
		return loadResolvedVersions(loader)
			.filter { resolved_version -> resolved_version.game_version == game_version_id }
			.asReversed()
			.map { resolved_version ->
				val display_name = resolved_version.version.removePrefix("$game_version_id-")

				LoaderVersion(
					version = resolved_version.version,
					display_name = display_name,
					is_stable = loader == Loader.FORGE || !SemanticVersions.isPreRelease(display_name),
					installer_url = resolved_version.installer_url
				)
			}
	}

	private fun mojangVersionIds(): Set<String> {
		return VersionStore.fetchManifest().versions.map { version -> version.id }.toSet()
	}

	private fun fetchGameVersionList(meta_url: String): Set<String> {
		val file_text = QuantumCache.fetchText(meta_url + "game")
		val entries = QuantumJson.json.decodeFromString(ListSerializer(LoaderGameVersion.serializer()), file_text)
		return entries.map { entry -> entry.version }.toSet()
	}

	internal fun forgeVersionMapping(): Map<String, List<String>> {
		val mapping_text = QuantumCache.fetchText(forge_mapping_url)
		return QuantumJson.json.decodeFromString(MapSerializer(String.serializer(), ListSerializer(String.serializer())), mapping_text)
	}

	private fun forgeMappedGameVersion(loader: Loader, forge_version: String): String? {
		if(loader != Loader.FORGE) {
			return null
		}

		return forgeVersionMapping().entries.firstOrNull { (_, forge_versions) -> forge_version in forge_versions }?.key
	}

	internal fun forgeInstallerUrl(forge_version: String): String {
		return "$forge_maven_url$forge_version/forge-$forge_version-installer.jar"
	}

	private fun saveSupportedGameVersions(loader: Loader, supported_versions: Set<String>) {
		val file_text = QuantumJson.json.encodeToString(ListSerializer(String.serializer()), supported_versions.toList())
		QuantumCache.writeText(supportedFileName(loader), file_text)
	}

	private fun fetchFabricStyleVersions(loader: Loader, meta_url: String, game_version_id: String): List<LoaderVersion> {
		val encoded_game_version = encodeForPath(game_version_id)
		val file_text = QuantumCache.fetchText("${meta_url}loader/$encoded_game_version")
		val entries = QuantumJson.json.decodeFromString(ListSerializer(FabricLoaderEntry.serializer()), file_text)

		return entries.map { entry ->
			val loader_version = entry.loader.version

			LoaderVersion(
				version = loader_version,
				display_name = loader_version,
				is_stable = entry.loader.stable,
				profile_url = "${meta_url}loader/$encoded_game_version/${encodeForPath(loader_version)}/profile/json"
			)
		}
	}

	private fun fetchQuiltVersions(game_version_id: String): List<LoaderVersion> {
		val encoded_game_version = encodeForPath(game_version_id)
		val file_text = QuantumCache.fetchText("${quilt_meta_url}loader/$encoded_game_version")
		val entries = QuantumJson.json.decodeFromString(ListSerializer(QuiltLoaderEntry.serializer()), file_text)

		return entries.map { entry ->
			val loader_version = entry.loader.version

			LoaderVersion(
				version = loader_version,
				display_name = loader_version,
				is_stable = !SemanticVersions.isPreRelease(loader_version),
				profile_url = "${quilt_meta_url}loader/$encoded_game_version/${encodeForPath(loader_version)}/profile/json"
			)
		}
	}

	private fun ornitheGenerations(): OrnitheGenerations {
		val generations_text = QuantumCache.fetchText("${ornithe_meta_url}intermediary_generations")
		return QuantumJson.json.decodeFromString(OrnitheGenerations.serializer(), generations_text)
	}

	private fun ornitheGameVersions(): Map<Int, Set<String>> {
		return (1..ornitheGenerations().latest_generation).associateWith { generation ->
			val game_text = QuantumCache.fetchText("${ornithe_meta_url}gen$generation/game")
			val game_versions = QuantumJson.json.decodeFromString(ListSerializer(OrnitheGameVersion.serializer()), game_text)

			game_versions
				.filter { game_version -> game_version.environment == "client" || game_version.environment == "*" }
				.map { game_version -> game_version.version }
				.toSet()
		}
	}

	private fun fetchOrnitheVersions(loader: Loader, game_version_id: String, generation: Int): List<LoaderVersion> {
		val loader_path = if(loader == Loader.ORNITHE_QUILT) "quilt-loader" else "fabric-loader"
		val encoded_game_version = encodeForPath(game_version_id)
		val base_url = "${ornithe_meta_url}gen$generation/$loader_path/"
		val file_text = QuantumCache.fetchText(base_url + encoded_game_version)
		val entries = QuantumJson.json.decodeFromString(ListSerializer(FabricLoaderEntry.serializer()), file_text)

		return entries.map { entry ->
			val loader_version = entry.loader.version

			LoaderVersion(
				version = loader_version,
				display_name = loader_version,
				is_stable = entry.loader.stable,
				profile_url = "$base_url$encoded_game_version/${encodeForPath(loader_version)}/profile/json"
			)
		}
	}

	private fun mavenArtifactList(base_url: String, artifact_name: String): List<MavenArtifact> {
		val metadata_text = QuantumCache.fetchText(base_url + "maven-metadata.xml")

		return parseMavenVersions(metadata_text).map { version ->
			MavenArtifact(version, "$base_url$version/$artifact_name-$version-installer.jar")
		}
	}

	private fun parseMavenVersions(metadata_text: String): List<String> {
		val document_factory = DocumentBuilderFactory.newInstance()
		document_factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)

		val document = document_factory.newDocumentBuilder().parse(InputSource(StringReader(metadata_text)))
		val version_nodes = document.getElementsByTagName("version")

		return (0 until version_nodes.length).map { index -> version_nodes.item(index).textContent.trim() }
	}

	private fun supportedFileName(loader: Loader): String {
		return "supported_game_versions_${loader.name.lowercase()}.json"
	}

	private fun resolvedFileName(loader: Loader): String {
		return "resolved_versions_${loader.name.lowercase()}.json"
	}

	private fun encodeForPath(text: String): String {
		return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20")
	}
}