package quantum.shared

import java.io.StringReader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.builtins.ListSerializer
import org.xml.sax.InputSource

object LoaderVersionStore {
	private const val fabric_loader_url = "https://meta.fabricmc.net/v2/versions/loader/"
	private const val fabric_game_url = "https://meta.fabricmc.net/v2/versions/game"
	private const val quilt_loader_url = "https://meta.quiltmc.org/v3/versions/loader/"
	private const val quilt_game_url = "https://meta.quiltmc.org/v3/versions/game"
	private const val neoforge_maven_url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/"
	private const val legacy_neoforge_maven_url = "https://maven.neoforged.net/releases/net/neoforged/forge/"
	private const val installer_check_threads = 8

	private val supported_game_versions_cache = ConcurrentHashMap<Loader, Set<String>>()
	private val maven_artifacts_cache = ConcurrentHashMap<Loader, List<MavenArtifact>>()
	private val game_version_maps = ConcurrentHashMap<Loader, Map<String, String?>>()

	fun hasVersionList(loader: Loader): Boolean {
		return loader == Loader.FABRIC || loader == Loader.QUILT || loader == Loader.NEOFORGE
	}

	fun fetchSupportedGameVersions(loader: Loader, onProgress: (Int, Int) -> Unit = { _, _ -> }): Set<String>? {
		val cached_versions = supported_game_versions_cache[loader]
		if(cached_versions != null) {
			return cached_versions
		}

		val supported_versions = when(loader) {
			Loader.FABRIC -> fetchGameVersionList(fabric_game_url)
			Loader.QUILT -> fetchGameVersionList(quilt_game_url)
			Loader.NEOFORGE -> gameVersionMap(loader, onProgress).values.filterNotNull().toSet()
			else -> return null
		}

		supported_game_versions_cache[loader] = supported_versions
		return supported_versions
	}

	fun fetchVersions(loader: Loader, game_version_id: String): List<LoaderVersion> {
		return when(loader) {
			Loader.FABRIC -> fetchFabricVersions(game_version_id)
			Loader.QUILT -> fetchQuiltVersions(game_version_id)
			Loader.NEOFORGE -> fetchMappedVersions(loader, game_version_id)
			else -> emptyList()
		}
	}

	private fun fetchGameVersionList(url: String): Set<String> {
		val file_text = QuantumHttp.fetchText(url)
		val entries = QuantumJson.json.decodeFromString(ListSerializer(LoaderGameVersion.serializer()), file_text)
		return entries.map { entry -> entry.version }.toSet()
	}

	private fun fetchFabricVersions(game_version_id: String): List<LoaderVersion> {
		val file_text = QuantumHttp.fetchText(fabric_loader_url + encodeForPath(game_version_id))
		val entries = QuantumJson.json.decodeFromString(ListSerializer(FabricLoaderEntry.serializer()), file_text)
		return entries.map { entry -> LoaderVersion(entry.loader.version, entry.loader.version, entry.loader.stable) }
	}

	private fun fetchQuiltVersions(game_version_id: String): List<LoaderVersion> {
		val file_text = QuantumHttp.fetchText(quilt_loader_url + encodeForPath(game_version_id))
		val entries = QuantumJson.json.decodeFromString(ListSerializer(QuiltLoaderEntry.serializer()), file_text)
		return entries.map { entry ->
			LoaderVersion(entry.loader.version, entry.loader.version, !SemanticVersions.isPreRelease(entry.loader.version))
		}
	}

	private fun fetchMappedVersions(loader: Loader, game_version_id: String): List<LoaderVersion> {
		val game_version_map = gameVersionMap(loader) { _, _ -> }

		return mavenArtifacts(loader)
			.asReversed()
			.filter { artifact -> game_version_map[artifact.version] == game_version_id }
			.map { artifact ->
				val display_name = artifact.version.removePrefix("$game_version_id-")
				LoaderVersion(artifact.version, display_name, !SemanticVersions.isPreRelease(display_name))
			}
	}

	private fun gameVersionMap(loader: Loader, onProgress: (Int, Int) -> Unit): Map<String, String?> {
		val cached_map = game_version_maps[loader]
		if(cached_map != null) {
			return cached_map
		}

		val resolved_map = resolveGameVersions(loader, mavenArtifacts(loader), onProgress)
		game_version_maps[loader] = resolved_map
		return resolved_map
	}

	private fun resolveGameVersions(loader: Loader, artifacts: List<MavenArtifact>, onProgress: (Int, Int) -> Unit): Map<String, String?> {
		val results = Collections.synchronizedMap(HashMap(LoaderGameVersionCache.load(loader)))
		val unchecked_artifacts = artifacts.filter { artifact -> !results.containsKey(artifact.version) }
		val checked_count = AtomicInteger(artifacts.size - unchecked_artifacts.size)

		onProgress(checked_count.get(), artifacts.size)

		if(unchecked_artifacts.isEmpty()) {
			return HashMap(results)
		}

		val executor = Executors.newFixedThreadPool(installer_check_threads)

		try {
			val tasks = unchecked_artifacts.map { artifact ->
				Callable {
					results[artifact.version] = InstallerInfo.minecraftVersion(artifact.installer_url)
					onProgress(checked_count.incrementAndGet(), artifacts.size)
				}
			}

			executor.invokeAll(tasks).forEach { future -> future.get() }
		} finally {
			executor.shutdownNow()
			LoaderGameVersionCache.save(loader, HashMap(results))
		}

		return HashMap(results)
	}

	private fun mavenArtifacts(loader: Loader): List<MavenArtifact> {
		val cached_artifacts = maven_artifacts_cache[loader]
		if(cached_artifacts != null) {
			return cached_artifacts
		}

		val artifacts = when(loader) {
			Loader.NEOFORGE -> mavenArtifactList(legacy_neoforge_maven_url, "forge") + mavenArtifactList(neoforge_maven_url, "neoforge")
			else -> emptyList()
		}

		maven_artifacts_cache[loader] = artifacts
		return artifacts
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

	private fun encodeForPath(game_version_id: String): String {
		return URLEncoder.encode(game_version_id, StandardCharsets.UTF_8).replace("+", "%20")
	}
}