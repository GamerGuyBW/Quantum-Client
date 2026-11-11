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

	fun launchMainClass(installer_url: String): String? {
		val profile_bytes = RemoteZip.readEntry(installer_url, "install_profile.json") ?: return null
		val profile = QuantumJson.json.parseToJsonElement(String(profile_bytes, Charsets.UTF_8)).jsonObject

		val original_format_main_class = profile["versionInfo"]?.jsonObject?.get("mainClass")?.jsonPrimitive?.contentOrNull
		if(original_format_main_class != null) {
			return original_format_main_class
		}

		val version_entry_name = profile["json"]?.jsonPrimitive?.contentOrNull?.removePrefix("/") ?: return null
		val version_bytes = RemoteZip.readEntry(installer_url, version_entry_name) ?: return null
		val version_profile = QuantumJson.json.parseToJsonElement(String(version_bytes, Charsets.UTF_8)).jsonObject

		return version_profile["mainClass"]?.jsonPrimitive?.contentOrNull
	}
}