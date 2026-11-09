package quantum.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainWindow() {
	Row(modifier = Modifier.fillMaxSize().background(QuantumColors.background)) {
		Sidebar()

		Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(QuantumColors.border))

		Box(modifier = Modifier.weight(1f).fillMaxHeight())
	}
}

@Composable
fun Sidebar() {
	Box(modifier = Modifier.width(220.dp).fillMaxHeight().background(QuantumColors.sidebar))
}