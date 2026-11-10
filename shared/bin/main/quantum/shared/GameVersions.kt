package quantum.shared

object GameVersions {
	const val latest_release = "latest_release"
	const val latest_snapshot = "latest_snapshot"

	fun displayName(game_version: String): String {
		return when(game_version) {
			latest_release -> "Latest Release"
			latest_snapshot -> "Latest Snapshot"
			else -> game_version
		}
	}

	fun resolve(game_version: String, manifest: VersionManifest): String {
		return when(game_version) {
			latest_release -> manifest.latest.release
			latest_snapshot -> manifest.latest.snapshot
			else -> game_version
		}
	}
}