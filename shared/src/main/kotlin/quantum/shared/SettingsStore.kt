package quantum.shared

import java.nio.file.Files
import java.nio.file.Path

object SettingsStore {
	private val settings_file: Path = QuantumPaths.data_folder.resolve("settings.json")

	fun load(): QuantumSettings {
		if(Files.notExists(settings_file)) {
			val default_settings = QuantumSettings()
			save(default_settings)
			return default_settings
		}

		val file_text = Files.readString(settings_file)
		return QuantumJson.json.decodeFromString(QuantumSettings.serializer(), file_text)
	}

	fun save(settings: QuantumSettings) {
		val file_text = QuantumJson.json.encodeToString(QuantumSettings.serializer(), settings)
		Files.writeString(settings_file, file_text)
	}
}