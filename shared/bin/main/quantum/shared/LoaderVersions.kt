package quantum.shared

import kotlinx.serialization.Serializable

data class LoaderVersion(
	val version: String,
	val is_stable: Boolean
)

@Serializable
internal data class FabricLoaderEntry(
	val loader: FabricLoaderInfo
)

@Serializable
internal data class FabricLoaderInfo(
	val version: String,
	val stable: Boolean
)

@Serializable
internal data class QuiltLoaderEntry(
	val loader: QuiltLoaderInfo
)

@Serializable
internal data class QuiltLoaderInfo(
	val version: String
)