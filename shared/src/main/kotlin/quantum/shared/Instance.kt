package quantum.shared

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Instance(
	val id: String = UUID.randomUUID().toString(),
	val name: String,
	val edition: Edition,
	val game_version: String,
	val loader: Loader = Loader.VANILLA,
	val loader_version: String? = null,
	val loader_version_name: String? = null,
	val group_id: String? = null,
	val addons: List<InstanceAddon> = emptyList()
)