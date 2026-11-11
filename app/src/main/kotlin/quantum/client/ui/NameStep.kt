package quantum.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import quantum.shared.Group

@Composable
fun NameStep(
	instance_name: String,
	onNameChange: (String) -> Unit,
	groups: List<Group>,
	selected_group_id: String?,
	onGroupSelected: (String?) -> Unit,
	modifier: Modifier = Modifier
) {
	val group_options = listOf(DropdownOption<String?>(null, "No group")) + groups.map { group -> DropdownOption<String?>(group.id, group.name) }

	Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
		QuantumTextField(label = "Name", value = instance_name, onValueChange = onNameChange)

		if(groups.isNotEmpty()) {
			QuantumDropdown(
				label = "Group",
				options = group_options,
				selected_value = selected_group_id,
				onValueSelected = onGroupSelected
			)
		}
	}
}