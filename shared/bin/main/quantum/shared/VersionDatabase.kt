package quantum.shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object VersionDatabase {
	@Synchronized
	fun load(loader: Loader): Map<String, String?> {
		val database_file = databaseFile(loader)
		if(Files.notExists(database_file)) {
			return emptyMap()
		}

		val lines = Files.readAllLines(database_file).filter { line -> line.isNotBlank() }
		val results = lines.mapNotNull { line ->
			runCatching { QuantumJson.compact_json.decodeFromString(InstallerResult.serializer(), line) }.getOrNull()
		}

		val result_map = results.associate { result -> result.version to result.minecraft_version }

		if(results.size != lines.size) {
			writeAll(loader, result_map)
		}

		return result_map
	}

	@Synchronized
	fun record(loader: Loader, version: String, minecraft_version: String?) {
		val result_line = QuantumJson.compact_json.encodeToString(InstallerResult.serializer(), InstallerResult(version, minecraft_version))
		Files.writeString(databaseFile(loader), result_line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
	}

	@Synchronized
	fun prune(loader: Loader, current_versions: Set<String>) {
		val existing_results = load(loader)
		val kept_results = existing_results.filterKeys { version -> version in current_versions }

		if(kept_results.size != existing_results.size) {
			writeAll(loader, kept_results)
		}
	}

	@Synchronized
	fun correct(loader: Loader, version: String, minecraft_version: String?) {
		writeAll(loader, load(loader) + (version to minecraft_version))
	}

	@Synchronized
	fun clear() {
		val database_folder = QuantumPaths.database_folder

		if(Files.exists(database_folder)) {
			Files.walk(database_folder).use { path_stream ->
				path_stream
					.sorted(Comparator.reverseOrder())
					.forEach { path -> Files.deleteIfExists(path) }
			}
		}

		Files.createDirectories(database_folder)
	}

	private fun writeAll(loader: Loader, results: Map<String, String?>) {
		val file_text = results.entries.joinToString(separator = "") { (version, minecraft_version) ->
			QuantumJson.compact_json.encodeToString(InstallerResult.serializer(), InstallerResult(version, minecraft_version)) + "\n"
		}

		AtomicFiles.writeText(databaseFile(loader), file_text)
	}

	private fun databaseFile(loader: Loader): Path {
		return QuantumPaths.database_folder.resolve("${loader.name.lowercase()}.jsonl")
	}
}