package quantum.shared

import kotlinx.serialization.Serializable

@Serializable
data class InstanceAddon(
	val addon: Addon,
	val version: String,
	val version_name: String
)