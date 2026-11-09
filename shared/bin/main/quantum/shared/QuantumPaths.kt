package quantum.shared

import java.nio.file.Files
import java.nio.file.Path

object QuantumPaths {
	val data_folder: Path = findDataFolder()
	val shared_folder: Path = data_folder.resolve("shared")
	val instances_folder: Path = data_folder.resolve("instances")

	fun createFolders() {
		Files.createDirectories(shared_folder)
		Files.createDirectories(instances_folder)
	}

	private fun findDataFolder(): Path {
		val home_folder = Path.of(System.getProperty("user.home"))
		val xdg_data_home = System.getenv("XDG_DATA_HOME")
		val base_folder = if(xdg_data_home.isNullOrBlank()) home_folder.resolve(".local").resolve("share") else Path.of(xdg_data_home)

		return base_folder.resolve(QuantumInfo.folder_name)
	}
}