package quantum.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import quantum.shared.Edition

@Composable
fun EditionPage(edition: Edition, modifier: Modifier = Modifier) {
	var is_creating by remember(edition) { mutableStateOf(false) }

	Box(modifier = modifier.padding(20.dp)) {
		if(is_creating) {
			CreateInstanceForm(
				edition = edition,
				onCreated = { is_creating = false },
				onCancel = { is_creating = false }
			)
		} else {
			Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
				BasicText(text = "${edition.display_name} instances", style = QuantumTextStyles.heading)
				QuantumButton(text = "Create instance", onClick = { is_creating = true })
			}
		}
	}
}