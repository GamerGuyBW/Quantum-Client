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
}