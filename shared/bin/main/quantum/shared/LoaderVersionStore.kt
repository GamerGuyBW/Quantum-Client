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
import org.xml.sax.InputSource

object LoaderVersionStore {
	private const val fabric_loader_url = "https://meta.fabricmc.net/v2/versions/loader/"
	private const val fabric_game_url = "https://meta.fabricmc.net/v2/versions/game"
	private const val quilt_loader_url = "https://meta.quiltmc.org/v3/versions/loader/"
	private const val quilt_game_url = "https://meta.quiltmc.org/v3/versions/game"
	private const val neoforge_maven_url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/"
	private const val legacy_neoforge_maven_url = "https://maven.neoforged.net/releases/net/neoforged/forge/"
	private const val forge_maven_url = "https://maven.minecraftforge.net/releases/net/minecraftforge/forge/"
	private const val forge_mapping_url = "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json"
	private const val forge_promotions_url = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json"
	private const val installer_read_threads = 16

	fun hasVersionList(loader: Loader): Boolean {
		return loader == Loader.FABRIC || loader == Loader.QUILT || loader == Loader.FORGE || loader == Loader.NEOFORGE
	}

	fun prepareLoader(loader: Loader) {
		when(loader) {
			Loader.FABRIC -> saveSupportedGameVersions(loader, fetchGameVersionList(fabric_game_url))
			Loader.QUILT -> saveSupportedGameVersions(loader, fetchGameVersionList(quilt_game_url))
			Loader.FORGE -> saveResolvedVersions(loader, resolveForgeVersions())
			Loader.NEOFORGE -> saveResolvedVersions(loader, resolveNeoForgeVersions())
			else -> throw IllegalArgumentException("`${loader.display_name}` does not have a version list.")
		}
	}

	fun loadSupportedGameVersions(loader: Loader): Set<String>? {
		val file_text = QuantumCache.readText(supportedFileName(loader)) ?: return null
		return QuantumJson.json.decodeFromString(ListSerializer(String.serializer()), file_text).toSet()
	}

	fun fetchVersions(loader: Loader, game_version_id: String): List<LoaderVersion> {
		return when(loader) {
			Loader.FABRIC -> fetchFabricVersions(game_version_id)
			Loader.QUILT -> fetchQuiltVersions(game_version_id)
			Loader.FORGE, Loader.NEOFORGE -> fetchResolvedVersions(loader, game_version_id)
			else -> emptyList()
		}
	}

	fun verifyGameVersion(loader: Loader, loader_version: LoaderVersion, expected_game_version: String): String? {
		val installer_url = loader_version.installer_url ?: return null
		val actual_game_version = InstallerInfo.minecraftVersion(installer_url) ?: forgeMappedGameVersion(loader, loader_version.version)

		if(actual_game_version == expected_game_version) {
			return null
		}

		return if(actual_game_version == null) {
			"${loader.display_name} `${loader_version.display_name}` is no longer available. Choose a different version."
		} else {
			"${loader.display_name} `${loader_version.display_name}` is for Minecraft `$actual_game_version`, not `$expected_game_version`. Choose a different version."
		}
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

	private fun <T, R> readInParallel(items: List<T>, read: (T) -> R): List<R> {
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
		val recommended_version = if(loader == Loader.FORGE) forgeRecommendedVersion(game_version_id) else null

		return loadResolvedVersions(loader)
			.filter { resolved_version -> resolved_version.game_version == game_version_id }
			.asReversed()
			.map { resolved_version ->
				val display_name = resolved_version.version.removePrefix("$game_version_id-")

				LoaderVersion(
					version = resolved_version.version,
					display_name = display_name,
					is_stable = loader == Loader.FORGE || !SemanticVersions.isPreRelease(display_name),
					installer_url = resolved_version.installer_url,
					is_recommended = display_name == recommended_version
				)
			}
	}

	private fun mojangVersionIds(): Set<String> {
		return VersionStore.fetchManifest().versions.map { version -> version.id }.toSet()
	}

	private fun fetchGameVersionList(url: String): Set<String> {
		val file_text = QuantumHttp.fetchText(url)
		val entries = QuantumJson.json.decodeFromString(ListSerializer(LoaderGameVersion.serializer()), file_text)
		return entries.map { entry -> entry.version }.toSet()
	}

	private fun forgeVersionMapping(): Map<String, List<String>> {
		val mapping_text = QuantumCache.textOrFetch("forge_version_mapping.json", forge_mapping_url)
		return QuantumJson.json.decodeFromString(MapSerializer(String.serializer(), ListSerializer(String.serializer())), mapping_text)
	}

	private fun forgeMappedGameVersion(loader: Loader, forge_version: String): String? {
		if(loader != Loader.FORGE) {
			return null
		}

		return forgeVersionMapping().entries.firstOrNull { (_, forge_versions) -> forge_version in forge_versions }?.key
	}

	private fun forgeRecommendedVersion(game_version_id: String): String? {
		val promotions_text = QuantumCache.textOrFetch("forge_promotions.json", forge_promotions_url)
		val promotions = QuantumJson.json.decodeFromString(ForgePromotions.serializer(), promotions_text)

		return promotions.promos["$game_version_id-recommended"]
	}

	private fun forgeInstallerUrl(forge_version: String): String {
		return "$forge_maven_url$forge_version/forge-$forge_version-installer.jar"
	}

	private fun saveSupportedGameVersions(loader: Loader, supported_versions: Set<String>) {
		val file_text = QuantumJson.json.encodeToString(ListSerializer(String.serializer()), supported_versions.toList())
		QuantumCache.writeText(supportedFileName(loader), file_text)
	}

	private fun fetchFabricVersions(game_version_id: String): List<LoaderVersion> {
		val cache_name = "loader_versions_fabric_${QuantumCache.safeName(game_version_id)}.json"
		val file_text = QuantumCache.textOrFetch(cache_name, fabric_loader_url + encodeForPath(game_version_id))
		val entries = QuantumJson.json.decodeFromString(ListSerializer(FabricLoaderEntry.serializer()), file_text)

		return entries.map { entry -> LoaderVersion(entry.loader.version, entry.loader.version, entry.loader.stable) }
	}

	private fun fetchQuiltVersions(game_version_id: String): List<LoaderVersion> {
		val cache_name = "loader_versions_quilt_${QuantumCache.safeName(game_version_id)}.json"
		val file_text = QuantumCache.textOrFetch(cache_name, quilt_loader_url + encodeForPath(game_version_id))
		val entries = QuantumJson.json.decodeFromString(ListSerializer(QuiltLoaderEntry.serializer()), file_text)

		return entries.map { entry ->
			LoaderVersion(entry.loader.version, entry.loader.version, !SemanticVersions.isPreRelease(entry.loader.version))
		}
	}

	private fun mavenArtifactList(base_url: String, artifact_name: String): List<MavenArtifact> {
		val metadata_text = QuantumHttp.fetchText(base_url + "maven-metadata.xml")

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

	private fun encodeForPath(game_version_id: String): String {
		return URLEncoder.encode(game_version_id, StandardCharsets.UTF_8).replace("+", "%20")
	}
}