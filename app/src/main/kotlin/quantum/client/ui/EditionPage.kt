package quantum.client.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import quantum.shared.Edition
import quantum.shared.GroupStore
import quantum.shared.Instance
import quantum.shared.InstanceStore

@Composable
fun EditionPage(edition: Edition, modifier: Modifier = Modifier) {
	var is_creating by remember(edition) { mutableStateOf(false) }
	var is_creating_group by remember(edition) { mutableStateOf(false) }
	var instances by remember(edition) { mutableStateOf(InstanceStore.loadForEdition(edition)) }
	var groups by remember(edition) { mutableStateOf(GroupStore.loadForEdition(edition)) }
	var selected_instance_id by remember(edition) { mutableStateOf<String?>(null) }
	var is_confirming_delete by remember(edition) { mutableStateOf(false) }
	var is_choosing_group by remember(edition) { mutableStateOf(false) }

	val selected_instance = instances.firstOrNull { instance -> instance.id == selected_instance_id }
	val group_ids = groups.map { group -> group.id }.toSet()
	val loose_instances = instances.filter { instance -> instance.group_id == null || instance.group_id !in group_ids }

	fun toggleSelection(instance: Instance) {
		selected_instance_id = if(selected_instance_id == instance.id) null else instance.id
		is_confirming_delete = false
		is_choosing_group = false
	}

	fun moveSelectedInstance(group_id: String?) {
		val instance_to_move = selected_instance
		if(instance_to_move != null) {
			InstanceStore.moveToGroup(instance_to_move, group_id)
			instances = InstanceStore.loadForEdition(edition)
		}
		is_choosing_group = false
	}

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
			Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					BasicText(text = "${edition.display_name} instances", style = QuantumTextStyles.heading)
					Spacer(modifier = Modifier.weight(1f))
					QuantumButton(text = "Create group", style = ButtonStyle.SECONDARY, onClick = { is_creating_group = true })
					QuantumButton(
						text = "Create instance",
						onClick = {
							is_creating_group = false
							is_creating = true
						}
					)
				}

				if(is_creating_group) {
					CreateGroupForm(
						edition = edition,
						onCreated = {
							groups = GroupStore.loadForEdition(edition)
							is_creating_group = false
						},
						onCancel = { is_creating_group = false }
					)
				}

				if(instances.isEmpty() && groups.isEmpty()) {
					BasicText(text = "No ${edition.display_name} instances yet. Create one to get started.", style = QuantumTextStyles.label)
				} else {
					LazyVerticalGrid(
						columns = GridCells.Adaptive(minSize = 200.dp),
						modifier = Modifier.weight(1f),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						verticalArrangement = Arrangement.spacedBy(10.dp)
					) {
						items(loose_instances, key = { instance -> instance.id }) { instance ->
							InstanceCard(
								instance = instance,
								is_selected = instance.id == selected_instance_id,
								onClick = { toggleSelection(instance) }
							)
						}

						groups.forEach { group ->
							val group_instances = instances.filter { instance -> instance.group_id == group.id }

							item(key = "group-${group.id}", span = { GridItemSpan(maxLineSpan) }) {
								BasicText(
									text = group.name,
									style = QuantumTextStyles.body.copy(fontWeight = FontWeight.Medium),
									modifier = Modifier.padding(top = 8.dp)
								)
							}

							if(group_instances.isEmpty()) {
								item(key = "empty-${group.id}", span = { GridItemSpan(maxLineSpan) }) {
									BasicText(text = "No instances in this group yet.", style = QuantumTextStyles.label)
								}
							} else {
								items(group_instances, key = { instance -> instance.id }) { instance ->
									InstanceCard(
										instance = instance,
										is_selected = instance.id == selected_instance_id,
										onClick = { toggleSelection(instance) }
									)
								}
							}
						}
					}
				}

				if(selected_instance != null) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						if(is_confirming_delete) {
							BasicText(
								text = "Delete `${selected_instance.name}`? Its worlds, mods and settings will be permanently deleted.",
								style = QuantumTextStyles.body,
								modifier = Modifier.weight(1f)
							)

							QuantumButton(
								text = "Delete permanently",
								style = ButtonStyle.DANGER,
								onClick = {
									InstanceStore.delete(selected_instance)
									instances = InstanceStore.loadForEdition(edition)
									selected_instance_id = null
									is_confirming_delete = false
								}
							)

							QuantumButton(text = "Cancel", style = ButtonStyle.SECONDARY, onClick = { is_confirming_delete = false })
						} else if(is_choosing_group) {
							BasicText(text = "Move `${selected_instance.name}` to:", style = QuantumTextStyles.body)

							Row(
								modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
								horizontalArrangement = Arrangement.spacedBy(8.dp)
							) {
								ChoiceChip(
									text = "No group",
									is_selected = selected_instance.group_id == null || selected_instance.group_id !in group_ids,
									onClick = { moveSelectedInstance(null) }
								)

								groups.forEach { group ->
									ChoiceChip(
										text = group.name,
										is_selected = selected_instance.group_id == group.id,
										onClick = { moveSelectedInstance(group.id) }
									)
								}
							}

							QuantumButton(text = "Cancel", style = ButtonStyle.SECONDARY, onClick = { is_choosing_group = false })
						} else {
							if(groups.isNotEmpty()) {
								QuantumButton(text = "Move to group", style = ButtonStyle.SECONDARY, onClick = { is_choosing_group = true })
							}

							QuantumButton(text = "Delete", style = ButtonStyle.DANGER, onClick = { is_confirming_delete = true })
						}
					}
				}
			}
		}
	}
}