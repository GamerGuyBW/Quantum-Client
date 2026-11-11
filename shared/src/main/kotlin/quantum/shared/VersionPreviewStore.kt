package quantum.shared

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class WikiVersionDetails(
	val name: String?,
	val image_file: String?
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
	private const val wiki_infobox_start = "{{Infobox version"
	private const val thumbnail_width = 320

	fun patchNoteImageUrl(version_id: String): String? {
		val notes = QuantumJson.json.decodeFromString(PatchNotes.serializer(), QuantumCache.fetchText(patch_notes_url))
		val image_path = notes.entries.firstOrNull { entry -> entry.version == version_id }?.image?.url ?: return null

		return URI.create(patch_notes_url).resolve(image_path).toString()
	}

	fun wikiDetails(version_id: String): WikiVersionDetails? {
		val page_title = wiki_page_prefix + version_id
		val page_url = "$wiki_api_url?action=parse&page=${encode(page_title)}&prop=wikitext&format=json&formatversion=2&redirects=1"
		val response = QuantumJson.json.parseToJsonElement(QuantumCache.fetchText(page_url)).jsonObject
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

	fun wikiImageUrl(image_file: String): String? {
		val image_url = "$wiki_api_url?action=query&titles=${encode("File:$image_file")}&prop=imageinfo&iiprop=url&iiurlwidth=$thumbnail_width&format=json&formatversion=2"
		val response = QuantumJson.json.parseToJsonElement(QuantumCache.fetchText(image_url)).jsonObject
		val page = response["query"]?.jsonObject?.get("pages")?.jsonArray?.firstOrNull()?.jsonObject ?: return null
		val image_info = page["imageinfo"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null

		return image_info["thumburl"]?.jsonPrimitive?.contentOrNull ?: image_info["url"]?.jsonPrimitive?.contentOrNull
	}

	fun loadImage(image_url: String): ByteArray {
		return QuantumCache.fetchBytes(image_url)
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

	private fun encode(text: String): String {
		return URLEncoder.encode(text, StandardCharsets.UTF_8)
	}
}