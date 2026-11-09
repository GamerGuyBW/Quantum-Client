package quantum.shared

import java.nio.file.Files
import java.nio.file.Path

object InstanceStore {
	private const val instance_file_name = "instance.json"

	fun instanceFolder(instance: Instance): Path {
		return QuantumPaths.instances_folder.resolve(instance.id)
	}

	fun save(instance: Instance) {
		val instance_folder = instanceFolder(instance)
		Files.createDirectories(instance_folder)

		val file_text = QuantumJson.json.encodeToString(Instance.serializer(), instance)
		Files.writeString(instance_folder.resolve(instance_file_name), file_text)
	}

	fun load(instance_folder: Path): Instance {
		val file_text = Files.readString(instance_folder.resolve(instance_file_name))
		return QuantumJson.json.decodeFromString(Instance.serializer(), file_text)
	}

	fun loadAll(): List<Instance> {
		if(Files.notExists(QuantumPaths.instances_folder)) {
			return emptyList()
		}

		val instance_folders = Files.list(QuantumPaths.instances_folder).use { folder_stream ->
			folder_stream.toList()
		}

		return instance_folders
			.filter { folder -> Files.exists(folder.resolve(instance_file_name)) }
			.map { folder -> load(folder) }
			.sortedBy { instance -> instance.name.lowercase() }
	}

	fun loadForEdition(edition: Edition): List<Instance> {
		return loadAll().filter { instance -> instance.edition == edition }
	}

	fun delete(instance: Instance) {
		val instances_folder = QuantumPaths.instances_folder.normalize()
		val instance_folder = instanceFolder(instance).normalize()

		require(instance_folder.parent == instances_folder && instance_folder.fileName.toString() == instance.id) {
			"Refusing to delete `$instance_folder` because it is not an instance folder."
		}

		if(Files.notExists(instance_folder)) {
			return
		}

		Files.walk(instance_folder).use { path_stream ->
			path_stream
				.sorted(Comparator.reverseOrder())
				.forEach { path -> Files.delete(path) }
		}
	}
}