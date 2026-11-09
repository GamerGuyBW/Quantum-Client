package quantum.client

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import quantum.client.ui.MainWindow
import quantum.shared.Edition
import quantum.shared.Instance
import quantum.shared.InstanceStore
import quantum.shared.Loader
import quantum.shared.QuantumInfo
import quantum.shared.QuantumPaths
import quantum.shared.SettingsStore

fun main() {
	QuantumPaths.createFolders()

	val settings = SettingsStore.load()
	println("Loaded settings (format version ${settings.format_version})")

	// Temporary Phase 7 test: removed in Phase 8.
	val test_instance = Instance(name = "Test", edition = Edition.JAVA, game_version = "latest", loader = Loader.FABRIC)
	InstanceStore.save(test_instance)

	val loaded_instance = InstanceStore.load(InstanceStore.instanceFolder(test_instance))
	println("Saved and loaded instance: ${loaded_instance.name} (${loaded_instance.id})")

	application {
		val window_state = rememberWindowState(size = DpSize(1280.dp, 720.dp))

		Window(onCloseRequest = ::exitApplication, title = QuantumInfo.app_name, state = window_state) {
			LaunchedEffect(Unit) {
				window.minimumSize = Dimension(960, 600)
			}

			MainWindow()
		}
	}
}