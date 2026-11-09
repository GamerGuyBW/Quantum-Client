package quantum.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import quantum.shared.QuantumInfo

fun main() = application {
	Window(onCloseRequest = ::exitApplication, title = "Quantum Client") {
		Box(modifier = Modifier.fillMaxSize().background(Color.White))
	}
}