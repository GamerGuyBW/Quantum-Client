package quantum.client.ui

import androidx.compose.foundation.layout.Arrangement
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
import quantum.shared.Group
import quantum.shared.GroupStore
import quantum.shared.NameRules

@Composable
fun CreateGroupForm(edition: Edition, onCreated: () -> Unit, onCancel: () -> Unit) {
	var group_name by remember { mutableStateOf("") }
	var error_message by remember { mutableStateOf<String?>(null) }

	Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
		QuantumTextField(
			label = "Group name",
			value = group_name,
			onValueChange = { new_value ->
				group_name = NameRules.cleanTyped(new_value)
				error_message = null
			}
		)

		QuantumButton(
			text = "Create",
			onClick = {
				val final_name = group_name.trimEnd()
				val taken_names = GroupStore.loadForEdition(edition).map { group -> group.name }
				val name_problem = NameRules.findProblem(final_name, "group", edition, taken_names)

				if(name_problem != null) {
					error_message = name_problem
				} else {
					GroupStore.add(Group(name = final_name, edition = edition))
					onCreated()
				}
			}
		)

		QuantumButton(text = "Cancel", onClick = onCancel, style = ButtonStyle.SECONDARY)

		val current_error = error_message
		if(current_error != null) {
			BasicText(text = current_error, style = QuantumTextStyles.error, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
		}
	}
}