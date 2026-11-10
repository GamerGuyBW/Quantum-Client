package quantum.shared

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.builtins.ListSerializer

object LoaderVersionStore {
	private const val fabric_loader_url = "https://meta.fabricmc.net/v2/versions/loader/"

	fun fetchFabricVersions(game_version_id: String): List<FabricLoaderInfo> {
		val encoded_id = URLEncoder.encode(game_version_id, StandardCharsets.UTF_8).replace("+", "%20")
		val file_text = QuantumHttp.fetchText(fabric_loader_url + encoded_id)
		val entries = QuantumJson.json.decodeFromString(ListSerializer(FabricLoaderEntry.serializer()), file_text)

		return entries.map { entry -> entry.loader }
	}
}