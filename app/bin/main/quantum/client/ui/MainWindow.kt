package quantum.client.ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import quantum.shared.Edition

@Composable
fun MainWindow() {
	var selected_edition by remember { mutableStateOf(Edition.JAVA) }

	CompositionLocalProvider(LocalScrollbarStyle provides quantum_scrollbar_style) {
		Row(modifier = Modifier.fillMaxSize().background(QuantumColours.background)) {
			Sidebar(
				selected_edition = selected_edition,
				onEditionSelected = { edition -> selected_edition = edition }
			)

			Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(QuantumColours.border))

			EditionPage(edition = selected_edition, modifier = Modifier.weight(1f).fillMaxHeight())
		}
	}
}