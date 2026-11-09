package quantum.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import quantum.shared.QuantumInfo
import quantum.shared.QuantumPaths
import quantum.shared.SettingsStore

fun main() {
	QuantumPaths.createFolders()

	val settings = SettingsStore.load()
	println("Loaded settings (format version ${settings.format_version})")

	application {
		Window(onCloseRequest = ::exitApplication, title = QuantumInfo.app_name) {
			Box(modifier = Modifier.fillMaxSize().background(Color.White))
		}
	}
}