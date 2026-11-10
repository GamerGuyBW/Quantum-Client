package quantum.shared

import kotlinx.serialization.Serializable

@Serializable
data class FabricLoaderEntry(
	val loader: FabricLoaderInfo
)

@Serializable
data class FabricLoaderInfo(
	val version: String,
	val stable: Boolean
)