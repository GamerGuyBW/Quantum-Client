package quantum.shared

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class VersionPreview(
	val name: String?,
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

internal data class WikiVersionDetails(
	val name: String?,
	val image_file: String?
)

object VersionPreviewStore {
	private const val patch_notes_url = "https://launchercontent.mojang.com/v2/javaPatchNotes.json"
	private const val wiki_api_url = "https://minecraft.wiki/api.php"
	private const val wiki_page_prefix = "Java Edition "
	private const val wiki_infobox_start = "{{Infobox version"
	private const val thumbnail_width = 320

	fun loadPreview(version_id: String): VersionPreview {
		val cache_name = "version_preview_${QuantumCache.safeName(version_id)}.json"
		val cached_text = QuantumCache.readText(cache_name)

		if(cached_text != null) {
			return QuantumJson.json.decodeFromString(VersionPreview.serializer(), cached_text)
		}

		val wiki_details = wikiDetails(version_id)
		val image_url = patchNoteImages()[version_id] ?: wiki_details?.image_file?.let { image_file -> wikiImageUrl(image_file) }
		val preview = VersionPreview(wiki_details?.name, image_url)

		QuantumCache.writeText(cache_name, QuantumJson.json.encodeToString(VersionPreview.serializer(), preview))
		return preview
	}

	fun loadThumbnail(image_url: String): ByteArray {
		val cache_name = "thumbnail_${sha256(image_url)}"
		val cached_bytes = QuantumCache.readBytes(cache_name)

		if(cached_bytes != null) {
			return cached_bytes
		}

		val image_bytes = QuantumHttp.fetchBytes(image_url).body
		QuantumCache.writeBytes(cache_name, image_bytes)
		return image_bytes
	}

	private fun patchNoteImages(): Map<String, String> {
		val notes_text = QuantumCache.textOrFetch("java_patch_notes.json", patch_notes_url)
		val notes = QuantumJson.json.decodeFromString(PatchNotes.serializer(), notes_text)
		val notes_uri = URI.create(patch_notes_url)

		return notes.entries
			.mapNotNull { entry -> entry.image?.let { image -> entry.version to notes_uri.resolve(image.url).toString() } }
			.toMap()
	}

	private fun wikiDetails(version_id: String): WikiVersionDetails? {
		val page_title = wiki_page_prefix + version_id
		val page_url = "$wiki_api_url?action=parse&page=${encode(page_title)}&prop=wikitext&format=json&formatversion=2&redirects=1"
		val response = QuantumJson.json.parseToJsonElement(QuantumHttp.fetchText(page_url)).jsonObject
		val parsed_page = response["parse"]?.jsonObject ?: return null
		val wikitext = parsed_page["wikitext"]?.jsonPrimitive?.contentOrNull ?: return null
		val infobox = infoboxFields(wikitext) ?: return null

		val was_redirected = (parsed_page["redirects"]?.jsonArray?.size ?: 0) > 0
		if(was_redirected) {
			val manifest_hash = VersionStore.fetchManifest().versions.firstOrNull { version -> version.id == version_id }?.sha1
			if(manifest_hash == null || infobox["jsonhash"] != manifest_hash) {
				return null
			}
		}

		val image_file = infobox["image"]?.removePrefix("File:")?.takeIf { image_name -> image_name.isNotBlank() }
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

	private fun wikiImageUrl(image_file: String): String? {
		val image_url = "$wiki_api_url?action=query&titles=${encode("File:$image_file")}&prop=imageinfo&iiprop=url&iiurlwidth=$thumbnail_width&format=json&formatversion=2"
		val response = QuantumJson.json.parseToJsonElement(QuantumHttp.fetchText(image_url)).jsonObject
		val page = response["query"]?.jsonObject?.get("pages")?.jsonArray?.firstOrNull()?.jsonObject ?: return null
		val image_info = page["imageinfo"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null

		return image_info["thumburl"]?.jsonPrimitive?.contentOrNull ?: image_info["url"]?.jsonPrimitive?.contentOrNull
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

	private fun encode(text: String): String {
		return URLEncoder.encode(text, StandardCharsets.UTF_8)
	}

	private fun sha256(text: String): String {
		val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
		return digest.joinToString("") { byte -> "%02x".format(byte) }
	}
}