package quantum.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import quantum.shared.Edition
import quantum.shared.InstanceStore

@Composable
fun EditionPage(edition: Edition, modifier: Modifier = Modifier) {
	var is_creating by remember(edition) { mutableStateOf(false) }
	var instances by remember(edition) { mutableStateOf(InstanceStore.loadForEdition(edition)) }

	Box(modifier = modifier.padding(20.dp)) {
		if(is_creating) {
			CreateInstanceForm(
				edition = edition,
				onCreated = {
					instances = InstanceStore.loadForEdition(edition)
					is_creating = false
				},
				onCancel = { is_creating = false }
			)
		} else {
			Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
				Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
					BasicText(text = "${edition.display_name} instances", style = QuantumTextStyles.heading)
					Spacer(modifier = Modifier.weight(1f))
					QuantumButton(text = "Create instance", onClick = { is_creating = true })
				}

				if(instances.isEmpty()) {
					BasicText(text = "No ${edition.display_name} instances yet. Create one to get started.", style = QuantumTextStyles.label)
				} else {
					LazyVerticalGrid(
						columns = GridCells.Adaptive(minSize = 200.dp),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						verticalArrangement = Arrangement.spacedBy(10.dp)
					) {
						items(instances, key = { instance -> instance.id }) { instance ->
							InstanceCard(instance = instance)
						}
					}
				}
			}
		}
	}
}