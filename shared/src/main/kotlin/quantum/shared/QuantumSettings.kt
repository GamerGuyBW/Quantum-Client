package quantum.shared

import kotlinx.serialization.Serializable

@Serializable
data class QuantumSettings(
	val format_version: Int = 1
)