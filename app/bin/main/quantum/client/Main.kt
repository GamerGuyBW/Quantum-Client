package quantum.client

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import quantum.client.ui.MainWindow
import quantum.shared.LoaderPreloader
import quantum.shared.QuantumCache
import quantum.shared.QuantumInfo
import quantum.shared.QuantumPaths
import quantum.shared.SettingsStore

fun main() {
	QuantumPaths.createFolders()
	QuantumCache.clear()

	Runtime.getRuntime().addShutdownHook(Thread {
		LoaderPreloader.stop()
		QuantumCache.clear()
	})

	val settings = SettingsStore.load()
	println("Loaded settings (format version ${settings.format_version})")

	LoaderPreloader.start()

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