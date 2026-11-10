package quantum.shared

import java.io.StringReader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.xml.sax.InputSource

object LoaderVersionStore {
	private const val fabric_loader_url = "https://meta.fabricmc.net/v2/versions/loader/"
	private const val fabric_game_url = "https://meta.fabricmc.net/v2/versions/game"
	private const val quilt_loader_url = "https://meta.quiltmc.org/v3/versions/loader/"
	private const val quilt_game_url = "https://meta.quiltmc.org/v3/versions/game"
	private const val neoforge_maven_url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/"
	private const val legacy_neoforge_maven_url = "https://maven.neoforged.net/releases/net/neoforged/forge/"

	fun hasVersionList(loader: Loader): Boolean {
		return loader == Loader.FABRIC || loader == Loader.QUILT || loader == Loader.NEOFORGE
	}

	fun usesInstallerChecks(loader: Loader): Boolean {
		return loader == Loader.NEOFORGE
	}

	fun downloadSupportedGameVersions(loader: Loader) {
		val game_list_url = when(loader) {
			Loader.FABRIC -> fabric_game_url
			Loader.QUILT -> quilt_game_url
			else -> throw IllegalArgumentException("`${loader.display_name}` does not publish a supported version list.")
		}

		val file_text = QuantumHttp.fetchText(game_list_url)
		val entries = QuantumJson.json.decodeFromString(ListSerializer(LoaderGameVersion.serializer()), file_text)
		saveSupportedGameVersions(loader, entries.map { entry -> entry.version }.toSet())
	}

	fun loadSupportedGameVersions(loader: Loader): Set<String>? {
		val file_text = QuantumCache.readText(supportedFileName(loader)) ?: return null
		return QuantumJson.json.decodeFromString(ListSerializer(String.serializer()), file_text).toSet()
	}

	fun fetchVersions(loader: Loader, game_version_id: String): List<LoaderVersion> {
		return when(loader) {
			Loader.FABRIC -> fetchFabricVersions(game_version_id)
			Loader.QUILT -> fetchQuiltVersions(game_version_id)
			Loader.NEOFORGE -> fetchMappedVersions(loader, game_version_id)
			else -> emptyList()
		}
	}

	internal fun mavenArtifacts(loader: Loader): List<MavenArtifact> {
		val cached_text = QuantumCache.readText(artifactsFileName(loader))
		if(cached_text != null) {
			return QuantumJson.json.decodeFromString(ListSerializer(MavenArtifact.serializer()), cached_text)
		}

		val artifacts = when(loader) {
			Loader.NEOFORGE -> mavenArtifactList(legacy_neoforge_maven_url, "forge") + mavenArtifactList(neoforge_maven_url, "neoforge")
			else -> emptyList()
		}

		QuantumCache.writeText(artifactsFileName(loader), QuantumJson.json.encodeToString(ListSerializer(MavenArtifact.serializer()), artifacts))
		return artifacts
	}

	internal fun checkedInstallerResults(loader: Loader): Map<String, String?> {
		return QuantumCache.readLines(resultsFileName(loader))
			.map { line -> QuantumJson.compact_json.decodeFromString(InstallerResult.serializer(), line) }
			.associate { result -> result.version to result.minecraft_version }
	}

	internal fun checkInstaller(loader: Loader, artifact: MavenArtifact) {
		val minecraft_version = InstallerInfo.minecraftVersion(artifact.installer_url)
		val result_line = QuantumJson.compact_json.encodeToString(InstallerResult.serializer(), InstallerResult(artifact.version, minecraft_version))
		QuantumCache.appendLine(resultsFileName(loader), result_line)
	}

	internal fun finishInstallerChecks(loader: Loader) {
		val supported_versions = checkedInstallerResults(loader).values.filterNotNull().toSet()
		saveSupportedGameVersions(loader, supported_versions)
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

	private fun fetchMappedVersions(loader: Loader, game_version_id: String): List<LoaderVersion> {
		val installer_results = checkedInstallerResults(loader)

		return mavenArtifacts(loader)
			.asReversed()
			.filter { artifact -> installer_results[artifact.version] == game_version_id }
			.map { artifact ->
				val display_name = artifact.version.removePrefix("$game_version_id-")
				LoaderVersion(artifact.version, display_name, !SemanticVersions.isPreRelease(display_name))
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

	private fun artifactsFileName(loader: Loader): String {
		return "maven_artifacts_${loader.name.lowercase()}.json"
	}

	private fun resultsFileName(loader: Loader): String {
		return "installer_results_${loader.name.lowercase()}.jsonl"
	}

	private fun encodeForPath(game_version_id: String): String {
		return URLEncoder.encode(game_version_id, StandardCharsets.UTF_8).replace("+", "%20")
	}
}