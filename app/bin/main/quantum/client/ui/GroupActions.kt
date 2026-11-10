package quantum.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import quantum.shared.Group
import quantum.shared.GroupStore
import quantum.shared.NameRules

private enum class GroupActionMode {
	MENU,
	RENAMING,
	CONFIRMING_DELETE
}

@Composable
fun GroupActions(
	group: Group,
	instance_count: Int,
	taken_names: List<String>,
	onChanged: () -> Unit,
	onClose: () -> Unit
) {
	var mode by remember(group.id) { mutableStateOf(GroupActionMode.MENU) }
	var new_name by remember(group.id) { mutableStateOf(group.name) }
	var error_message by remember(group.id) { mutableStateOf<String?>(null) }

	val row_alignment = if(mode == GroupActionMode.RENAMING) Alignment.Bottom else Alignment.CenterVertically
	val instance_word = if(instance_count == 1) "instance" else "instances"

	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		verticalAlignment = row_alignment
	) {
		when(mode) {
			GroupActionMode.MENU -> {
				BasicText(text = "Group `${group.name}`", style = QuantumTextStyles.body)
				Spacer(modifier = Modifier.weight(1f))

				QuantumButton(
					text = "Rename",
					style = ButtonStyle.SECONDARY,
					onClick = {
						new_name = group.name
						error_message = null
						mode = GroupActionMode.RENAMING
					}
				)

				QuantumButton(text = "Delete group", style = ButtonStyle.DANGER, onClick = { mode = GroupActionMode.CONFIRMING_DELETE })
				QuantumButton(text = "Close", style = ButtonStyle.SECONDARY, onClick = onClose)
			}

			GroupActionMode.RENAMING -> {
				QuantumTextField(
					label = "New name for `${group.name}`",
					value = new_name,
					onValueChange = { new_value ->
						new_name = NameRules.cleanTyped(new_value)
						error_message = null
					}
				)

				QuantumButton(
					text = "Save",
					onClick = {
						val final_name = new_name.trimEnd()

						if(final_name == group.name) {
							mode = GroupActionMode.MENU
						} else {
							val name_problem = NameRules.findProblem(final_name, "group", group.edition, taken_names)

							if(name_problem != null) {
								error_message = name_problem
							} else {
								GroupStore.rename(group.id, final_name)
								onChanged()
								mode = GroupActionMode.MENU
							}
						}
					}
				)

				QuantumButton(text = "Cancel", style = ButtonStyle.SECONDARY, onClick = { mode = GroupActionMode.MENU })

				val current_error = error_message
				if(current_error != null) {
					BasicText(text = current_error, style = QuantumTextStyles.error, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
				}
			}

			GroupActionMode.CONFIRMING_DELETE -> {
				if(instance_count == 0) {
					BasicText(
						text = "Delete group `${group.name}`? It has no instances.",
						style = QuantumTextStyles.body,
						modifier = Modifier.weight(1f)
					)

					QuantumButton(
						text = "Delete group",
						style = ButtonStyle.DANGER,
						onClick = {
							GroupStore.delete(group, delete_instances = false)
							onChanged()
						}
					)
				} else {
					BasicText(
						text = "Delete group `${group.name}`? It contains $instance_count $instance_word. Choose whether to move them out of the group or permanently delete them too.",
						style = QuantumTextStyles.body,
						modifier = Modifier.weight(1f)
					)

					QuantumButton(
						text = "Move instances out",
						style = ButtonStyle.SECONDARY,
						onClick = {
							GroupStore.delete(group, delete_instances = false)
							onChanged()
						}
					)

					QuantumButton(
						text = "Delete instances too",
						style = ButtonStyle.DANGER,
						onClick = {
							GroupStore.delete(group, delete_instances = true)
							onChanged()
						}
					)
				}

				QuantumButton(text = "Cancel", style = ButtonStyle.SECONDARY, onClick = { mode = GroupActionMode.MENU })
			}
		}
	}
}