package quantum.shared

import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object InstallerInfo {
	fun minecraftVersion(installer_url: String): String? {
		val profile_bytes = RemoteZip.readEntry(installer_url, "install_profile.json") ?: return null
		val profile = QuantumJson.json.parseToJsonElement(String(profile_bytes, Charsets.UTF_8)).jsonObject

		val current_format_version = profile["minecraft"]?.jsonPrimitive?.contentOrNull
		val original_format_version = profile["install"]?.jsonObject?.get("minecraft")?.jsonPrimitive?.contentOrNull

		return current_format_version ?: original_format_version
	}
}