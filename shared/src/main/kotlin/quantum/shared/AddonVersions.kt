package quantum.shared

import kotlinx.serialization.Serializable

@Serializable
internal data class LiteLoaderBuild(
	val version: String,
	val build: String? = null,
	val file: String,
	val md5: String,
	val stream: String,
	val timestamp: String
)

@Serializable
internal data class AddonSupport(
	val loader: Loader,
	val game_versions: List<String>
)