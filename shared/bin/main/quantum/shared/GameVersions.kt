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
}