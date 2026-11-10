package quantum.shared

object VersionStore {
	private const val manifest_url = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
	private const val manifest_cache_name = "version_manifest.json"

	fun fetchManifest(): VersionManifest {
		val file_text = QuantumCache.textOrFetch(manifest_cache_name, manifest_url)
		return QuantumJson.json.decodeFromString(VersionManifest.serializer(), file_text)
	}
}