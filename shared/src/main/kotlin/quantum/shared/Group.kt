package quantum.shared

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Group(
	val id: String = UUID.randomUUID().toString(),
	val name: String,
	val edition: Edition,
	val is_collapsed: Boolean = false
)