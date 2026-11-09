package quantum.client.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import quantum.shared.Edition

@Composable
fun EditionPage(edition: Edition, modifier: Modifier = Modifier) {
	Box(modifier = modifier.padding(20.dp)) {
		BasicText(
			text = "${edition.display_name} instances",
			style = TextStyle(color = QuantumColors.text_primary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
		)
	}
}