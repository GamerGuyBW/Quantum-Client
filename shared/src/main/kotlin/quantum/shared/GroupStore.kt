package quantum.shared

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable

@Serializable
private data class GroupsFile(
	val groups: List<Group> = emptyList()
)

object GroupStore {
	private val groups_file: Path = QuantumPaths.data_folder.resolve("groups.json")

	fun loadAll(): List<Group> {
		if(Files.notExists(groups_file)) {
			return emptyList()
		}

		val file_text = Files.readString(groups_file)
		return QuantumJson.json.decodeFromString(GroupsFile.serializer(), file_text).groups
	}

	fun loadForEdition(edition: Edition): List<Group> {
		return loadAll().filter { group -> group.edition == edition }
	}

	fun add(group: Group) {
		saveAll(loadAll() + group)
	}

	fun setCollapsed(group_id: String, is_collapsed: Boolean) {
		val updated_groups = loadAll().map { group ->
			if(group.id == group_id) group.copy(is_collapsed = is_collapsed) else group
		}
		saveAll(updated_groups)
	}

	private fun saveAll(groups: List<Group>) {
		val file_text = QuantumJson.json.encodeToString(GroupsFile.serializer(), GroupsFile(groups))
		Files.writeString(groups_file, file_text)
	}
}