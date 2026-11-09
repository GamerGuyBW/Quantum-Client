package quantum.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import quantum.shared.Instance
import quantum.shared.InstanceStore
import quantum.shared.Loader
import quantum.shared.NameRules

@Composable
fun CreateInstanceForm(edition: Edition, onCreated: () -> Unit, onCancel: () -> Unit) {
	var instance_name by remember { mutableStateOf("") }
	var game_version by remember { mutableStateOf("latest") }
	var selected_loader by remember { mutableStateOf(Loader.VANILLA) }
	var error_message by remember { mutableStateOf<String?>(null) }

	Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
		BasicText(text = "New ${edition.display_name} instance", style = QuantumTextStyles.heading)

		QuantumTextField(
			label = "Name",
			value = instance_name,
			onValueChange = { new_value ->
				instance_name = NameRules.cleanTyped(new_value)
				error_message = null
			}
		)

		QuantumTextField(
			label = "Game version",
			value = game_version,
			onValueChange = { new_value ->
				game_version = new_value
				error_message = null
			}
		)

		if(edition == Edition.JAVA) {
			Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
				BasicText(text = "Mod loader", style = QuantumTextStyles.label)

				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					Loader.entries.forEach { loader ->
						ChoiceChip(
							text = loader.display_name,
							is_selected = loader == selected_loader,
							onClick = { selected_loader = loader }
						)
					}
				}
			}
		}

		Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
			QuantumButton(
				text = "Create",
				onClick = {
					val final_name = instance_name.trimEnd()
					val taken_names = InstanceStore.loadForEdition(edition).map { instance -> instance.name }
					val name_problem = NameRules.findProblem(final_name, "instance", edition, taken_names)

					if(name_problem != null) {
						error_message = name_problem
					} else if(game_version.isBlank()) {
						error_message = "Enter a game version, or `latest`."
					} else {
						val new_instance = Instance(
							name = final_name,
							edition = edition,
							game_version = game_version.trim(),
							loader = selected_loader
						)
						InstanceStore.save(new_instance)
						onCreated()
					}
				}
			)

			QuantumButton(text = "Cancel", onClick = onCancel, style = ButtonStyle.SECONDARY)

			val current_error = error_message
			if(current_error != null) {
				BasicText(text = current_error, style = QuantumTextStyles.error, modifier = Modifier.padding(start = 6.dp))
			}
		}
	}
}