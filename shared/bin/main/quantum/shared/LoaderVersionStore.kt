package quantum.shared

import java.io.StringReader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.builtins.ListSerializer
import org.xml.sax.InputSource

object LoaderVersionStore {
	private const val fabric_loader_url = "https://meta.fabricmc.net/v2/versions/loader/"
	private const val fabric_game_url = "https://meta.fabricmc.net/v2/versions/game"
	private const val quilt_loader_url = "https://meta.quiltmc.org/v3/versions/loader/"
	private const val quilt_game_url = "https://meta.quiltmc.org/v3/versions/game"
	private const val neoforge_metadata_url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"

	private val supported_game_versions_cache = ConcurrentHashMap<Loader, Set<String>>()

	@Volatile
	private var neoforge_versions_cache: List<String>? = null

	fun hasVersionList(loader: Loader): Boolean {
		return loader == Loader.FABRIC || loader == Loader.QUILT || loader == Loader.NEOFORGE
	}

	fun fetchSupportedGameVersions(loader: Loader): Set<String>? {
		val cached_versions = supported_game_versions_cache[loader]
		if(cached_versions != null) {
			return cached_versions
		}

		val supported_versions = when(loader) {
			Loader.FABRIC -> fetchGameVersionList(fabric_game_url)
			Loader.QUILT -> fetchGameVersionList(quilt_game_url)
			Loader.NEOFORGE -> fetchNeoForgeVersionList().mapNotNull { neoforge_version -> neoForgeGameVersion(neoforge_version) }.toSet()
			else -> return null
		}

		supported_game_versions_cache[loader] = supported_versions
		return supported_versions
	}

	fun fetchVersions(loader: Loader, game_version_id: String): List<LoaderVersion> {
		return when(loader) {
			Loader.FABRIC -> fetchFabricVersions(game_version_id)
			Loader.QUILT -> fetchQuiltVersions(game_version_id)
			Loader.NEOFORGE -> fetchNeoForgeVersions(game_version_id)
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

	private fun fetchNeoForgeVersions(game_version_id: String): List<LoaderVersion> {
		return fetchNeoForgeVersionList()
			.filter { neoforge_version -> neoForgeGameVersion(neoforge_version) == game_version_id }
			.sortedWith { first_version, second_version -> compareLoaderVersions(second_version, first_version) }
			.map { neoforge_version -> LoaderVersion(neoforge_version, !neoforge_version.contains("-")) }
	}

	private fun fetchNeoForgeVersionList(): List<String> {
		val cached_versions = neoforge_versions_cache
		if(cached_versions != null) {
			return cached_versions
		}

		val metadata_text = QuantumHttp.fetchText(neoforge_metadata_url)
		val version_list = parseMavenVersions(metadata_text)

		neoforge_versions_cache = version_list
		return version_list
	}

	private fun neoForgeGameVersion(neoforge_version: String): String? {
		val build_tag = neoforge_version.substringAfter("+", "")
		val number_part = neoforge_version.substringBefore("-").substringBefore("+")
		val numbers = number_part.split(".").mapNotNull { part -> part.toIntOrNull() }

		if(numbers.size < 3) {
			return null
		}

		val major = numbers[0]
		val minor = numbers[1]
		val patch = numbers[2]

		if(major >= 26) {
			if(numbers.size < 4) {
				return null
			}

			if(build_tag.isNotEmpty()) {
				return "$major.$minor-$build_tag"
			}

			return if(patch == 0) "$major.$minor" else "$major.$minor.$patch"
		}

		return if(minor == 0) "1.$major" else "1.$major.$minor"
	}

	private fun parseMavenVersions(metadata_text: String): List<String> {
		val document_factory = DocumentBuilderFactory.newInstance()
		document_factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)

		val document = document_factory.newDocumentBuilder().parse(InputSource(StringReader(metadata_text)))
		val version_nodes = document.getElementsByTagName("version")

		return (0 until version_nodes.length).map { index -> version_nodes.item(index).textContent.trim() }
	}

	private fun compareLoaderVersions(first_version: String, second_version: String): Int {
		val first_numbers = versionNumbers(first_version)
		val second_numbers = versionNumbers(second_version)

		val first_difference = first_numbers.zip(second_numbers)
			.map { (first_number, second_number) -> first_number.compareTo(second_number) }
			.firstOrNull { difference -> difference != 0 }

		return first_difference ?: first_numbers.size.compareTo(second_numbers.size)
	}

	private fun versionNumbers(version: String): List<Long> {
		return Regex("\\d+").findAll(version).mapNotNull { match -> match.value.toLongOrNull() }.toList()
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