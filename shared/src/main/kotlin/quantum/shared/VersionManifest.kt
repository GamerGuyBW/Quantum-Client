package quantum.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VersionManifest(
	val latest: LatestVersions,
	val versions: List<GameVersion>
)

@Serializable
data class LatestVersions(
	val release: String,
	val snapshot: String
)

@Serializable
data class GameVersion(
	val id: String,
	val type: String,
	val url: String,
	@SerialName("releaseTime") val release_time: String,
	val sha1: String
)