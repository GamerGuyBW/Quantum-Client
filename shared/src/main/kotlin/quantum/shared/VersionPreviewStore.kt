package quantum.shared

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class WikiVersionDetails(
	val name: String?,
	val image_file: String?
)

@Serializable
internal data class WikiDetailsRecord(
	val details: WikiVersionDetails?
)

@Serializable
internal data class WikiImageRecord(
	val image_url: String?
)

@Serializable
internal data class PatchNotes(
	val entries: List<PatchNoteEntry>
)

@Serializable
internal data class PatchNoteEntry(
	val version: String,
	val image: PatchNoteImage? = null
)

@Serializable
internal data class PatchNoteImage(
	val url: String
)

object VersionPreviewStore {
	private const val patch_notes_url = "https://launchercontent.mojang.com/v2/javaPatchNotes.json"
	private const val wiki_api_url = "https://minecraft.wiki/api.php"
	private const val wiki_page_prefix = "Java Edition "
	private const val wiki_file_prefix = "File:"
	private const val wiki_infobox_start = "{{Infobox version"
	private const val wiki_titles_per_request = 50
	private const val thumbnail_width = 320

	fun patchNoteImageUrls(version_ids: Collection<String>, allow_download: Boolean = true): Map<String, String?> {
		val notes_text = if(allow_download) QuantumCache.fetchText(patch_notes_url) else QuantumCache.readCachedText(patch_notes_url) ?: return emptyMap()
		val notes = QuantumJson.json.decodeFromString(PatchNotes.serializer(), notes_text)
		val notes_uri = URI.create(patch_notes_url)
		val image_paths = notes.entries
			.mapNotNull { entry -> entry.image?.let { image -> entry.version to image.url } }
			.toMap()

		return version_ids.associateWith { version_id -> image_paths[version_id]?.let { image_path -> notes_uri.resolve(image_path).toString() } }
	}

	fun wikiDetails(version_ids: Collection<String>, allow_download: Boolean = true): Map<String, WikiVersionDetails?> {
		val results = mutableMapOf<String, WikiVersionDetails?>()
		val uncached_ids = mutableListOf<String>()

		version_ids.distinct().forEach { version_id ->
			val record_text = QuantumCache.readRecord(wikiDetailsSource(version_id))

			if(record_text != null) {
				results[version_id] = QuantumJson.json.decodeFromString(WikiDetailsRecord.serializer(), record_text).details
			} else {
				uncached_ids.add(version_id)
			}
		}

		if(!allow_download) {
			return results
		}

		uncached_ids.chunked(wiki_titles_per_request).forEach { id_chunk ->
			fetchWikiDetails(id_chunk).forEach { (version_id, details) ->
				QuantumCache.writeRecord(wikiDetailsSource(version_id), QuantumJson.compact_json.encodeToString(WikiDetailsRecord.serializer(), WikiDetailsRecord(details)))
				results[version_id] = details
			}
		}

		return results
	}

	fun wikiImageUrls(image_files: Collection<String>, allow_download: Boolean = true): Map<String, String?> {
		val results = mutableMapOf<String, String?>()
		val uncached_files = mutableListOf<String>()

		image_files.distinct().forEach { image_file ->
			val record_text = QuantumCache.readRecord(wikiImageSource(image_file))

			if(record_text != null) {
				results[image_file] = QuantumJson.json.decodeFromString(WikiImageRecord.serializer(), record_text).image_url
			} else {
				uncached_files.add(image_file)
			}
		}

		if(!allow_download) {
			return results
		}

		uncached_files.chunked(wiki_titles_per_request).forEach { file_chunk ->
			fetchWikiImageUrls(file_chunk).forEach { (image_file, image_url) ->
				QuantumCache.writeRecord(wikiImageSource(image_file), QuantumJson.compact_json.encodeToString(WikiImageRecord.serializer(), WikiImageRecord(image_url)))
				results[image_file] = image_url
			}
		}

		return results
	}

	fun loadImage(image_url: String): ByteArray {
		return QuantumCache.fetchBytes(image_url)
	}

	fun cachedImage(image_url: String): ByteArray? {
		return QuantumCache.readCachedBytes(image_url)
	}

	private fun fetchWikiDetails(version_ids: List<String>): Map<String, WikiVersionDetails?> {
		val titles = version_ids.associateBy { version_id -> wiki_page_prefix + version_id }
		val query_url = "$wiki_api_url?action=query&prop=revisions&rvprop=content&rvslots=main&redirects=1&format=json&formatversion=2&titles=${encode(titles.keys.joinToString("|"))}"
		val query = QuantumJson.json.parseToJsonElement(QuantumCache.fetchText(query_url)).jsonObject["query"]?.jsonObject
		val normalized_titles = titleMapping(query, "normalized")
		val redirected_titles = titleMapping(query, "redirects")
		val manifest_hashes = VersionStore.fetchManifest().versions.associate { version -> version.id to version.sha1 }

		val page_contents = query?.get("pages")?.jsonArray.orEmpty().mapNotNull { page_element ->
			val page = page_element.jsonObject
			val page_title = page["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
			val content = page["revisions"]?.jsonArray?.firstOrNull()?.jsonObject
				?.get("slots")?.jsonObject
				?.get("main")?.jsonObject
				?.get("content")?.jsonPrimitive?.contentOrNull

			if(content == null) null else page_title to content
		}.toMap()

		return titles.entries.associate { (title, version_id) ->
			val normalized_title = normalized_titles[title] ?: title
			val target_title = redirected_titles[normalized_title] ?: normalized_title
			val content = page_contents[target_title]
			val details = content?.let { page_content -> detailsFromWikitext(page_content, target_title != normalized_title, manifest_hashes[version_id]) }

			version_id to details
		}
	}

	private fun fetchWikiImageUrls(image_files: List<String>): Map<String, String?> {
		val titles = image_files.associateBy { image_file -> wiki_file_prefix + image_file }
		val query_url = "$wiki_api_url?action=query&prop=imageinfo&iiprop=url&iiurlwidth=$thumbnail_width&redirects=1&format=json&formatversion=2&titles=${encode(titles.keys.joinToString("|"))}"
		val query = QuantumJson.json.parseToJsonElement(QuantumCache.fetchText(query_url)).jsonObject["query"]?.jsonObject
		val normalized_titles = titleMapping(query, "normalized")
		val redirected_titles = titleMapping(query, "redirects")

		val page_urls = query?.get("pages")?.jsonArray.orEmpty().mapNotNull { page_element ->
			val page = page_element.jsonObject
			val page_title = page["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
			val image_info = page["imageinfo"]?.jsonArray?.firstOrNull()?.jsonObject
			val image_url = image_info?.get("thumburl")?.jsonPrimitive?.contentOrNull ?: image_info?.get("url")?.jsonPrimitive?.contentOrNull

			if(image_url == null) null else page_title to image_url
		}.toMap()

		return titles.entries.associate { (title, image_file) ->
			val normalized_title = normalized_titles[title] ?: title
			val target_title = redirected_titles[normalized_title] ?: normalized_title

			image_file to page_urls[target_title]
		}
	}

	private fun titleMapping(query: JsonObject?, field_name: String): Map<String, String> {
		return query?.get(field_name)?.jsonArray.orEmpty().mapNotNull { mapping_element ->
			val mapping = mapping_element.jsonObject
			val from_title = mapping["from"]?.jsonPrimitive?.contentOrNull
			val to_title = mapping["to"]?.jsonPrimitive?.contentOrNull

			if(from_title == null || to_title == null) null else from_title to to_title
		}.toMap()
	}

	private fun detailsFromWikitext(wikitext: String, was_redirected: Boolean, manifest_hash: String?): WikiVersionDetails? {
		val infobox = infoboxFields(wikitext) ?: return null

		if(was_redirected && (manifest_hash == null || infobox["jsonhash"] != manifest_hash)) {
			return null
		}

		val image_file = infobox["image"]?.removePrefix(wiki_file_prefix)?.takeIf { image_name -> image_name.isNotBlank() }
		return WikiVersionDetails(infobox["name"]?.let { name -> cleanWikiText(name) }, image_file)
	}

	private fun infoboxFields(wikitext: String): Map<String, String>? {
		val start_index = wikitext.indexOf(wiki_infobox_start)
		if(start_index < 0) {
			return null
		}

		val end_index = wikitext.indexOf("\n}}", start_index).takeIf { index -> index >= 0 } ?: wikitext.length

		return wikitext.substring(start_index, end_index)
			.lines()
			.map { line -> line.trim() }
			.filter { line -> line.startsWith("|") && line.contains("=") }
			.associate { line -> line.removePrefix("|").substringBefore("=").trim() to line.substringAfter("=").trim() }
	}

	private fun cleanWikiText(text: String): String? {
		val cleaned_text = text
			.replace(Regex("""\[\[(?:[^\]|]*\|)?([^\]]*)\]\]"""), "$1")
			.replace(Regex("""\{\{[^}]*\}\}"""), "")
			.replace(Regex("""<[^>]+>"""), "")
			.replace(Regex("""'{2,}"""), "")
			.trim()

		return cleaned_text.ifEmpty { null }
	}

	private fun wikiDetailsSource(version_id: String): String {
		return "wiki_details:$version_id"
	}

	private fun wikiImageSource(image_file: String): String {
		return "wiki_image:$image_file"
	}

	private fun encode(text: String): String {
		return URLEncoder.encode(text, StandardCharsets.UTF_8)
	}
}