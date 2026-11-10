package quantum.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import quantum.shared.Edition
import quantum.shared.GameVersions
import quantum.shared.GroupStore
import quantum.shared.Instance
import quantum.shared.InstanceStore
import quantum.shared.Loader
import quantum.shared.NameRules
import quantum.shared.VersionManifest
import quantum.shared.VersionStore

@Composable
fun CreateInstanceForm(edition: Edition, onCreated: () -> Unit, onCancel: () -> Unit) {
	val groups = remember(edition) { GroupStore.loadForEdition(edition) }
	val group_options = listOf(DropdownOption<String?>(null, "No group")) + groups.map { group -> DropdownOption<String?>(group.id, group.name) }

	var instance_name by remember { mutableStateOf("") }
	var game_version by remember { mutableStateOf(if(edition == Edition.JAVA) GameVersions.latest_release else "latest") }
	var selected_loader by remember { mutableStateOf(Loader.VANILLA) }
	var selected_group_id by remember { mutableStateOf<String?>(null) }
	var error_message by remember { mutableStateOf<String?>(null) }

	var manifest by remember { mutableStateOf<VersionManifest?>(null) }
	var version_load_failed by remember { mutableStateOf(false) }
	var retry_count by remember { mutableIntStateOf(0) }

	val loaded_manifest = manifest
	val version_options = remember(loaded_manifest) {
		if(loaded_manifest != null) versionOptions(loaded_manifest) else emptyList()
	}

	LaunchedEffect(edition, retry_count) {
		if(edition == Edition.JAVA) {
			version_load_failed = false

			try {
				manifest = withContext(Dispatchers.IO) { VersionStore.fetchManifest() }
			} catch(error: CancellationException) {
				throw error
			} catch(error: Exception) {
				version_load_failed = true
			}
		}
	}

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

		if(edition == Edition.JAVA) {
			if(loaded_manifest != null) {
				QuantumDropdown(
					label = "Game version",
					options = version_options,
					selected_value = game_version,
					onValueSelected = { version_id ->
						game_version = version_id
						error_message = null
					}
				)
			} else {
				Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
					BasicText(text = "Game version", style = QuantumTextStyles.label)

					if(version_load_failed) {
						Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
							BasicText(text = "Could not load the version list. Check your internet connection.", style = QuantumTextStyles.error)
							QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = { retry_count += 1 })
						}
					} else {
						BasicText(text = "Loading the version list…", style = QuantumTextStyles.body)
					}
				}
			}
		} else {
			QuantumTextField(
				label = "Game version",
				value = game_version,
				onValueChange = { new_value ->
					game_version = new_value
					error_message = null
				}
			)
		}

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

		if(groups.isNotEmpty()) {
			QuantumDropdown(
				label = "Group",
				options = group_options,
				selected_value = selected_group_id,
				onValueSelected = { group_id -> selected_group_id = group_id }
			)
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
					} else if(edition == Edition.JAVA && manifest == null) {
						error_message = "Wait for the version list to load before creating the instance."
					} else if(game_version.isBlank()) {
						error_message = "Enter a game version, or `latest`."
					} else {
						val new_instance = Instance(
							name = final_name,
							edition = edition,
							game_version = game_version.trim(),
							loader = selected_loader,
							group_id = selected_group_id
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

private fun versionOptions(manifest: VersionManifest): List<DropdownOption<String>> {
	val latest_options = listOf(
		DropdownOption(GameVersions.latest_release, "Latest Release (${manifest.latest.release})"),
		DropdownOption(GameVersions.latest_snapshot, "Latest Snapshot (${manifest.latest.snapshot})")
	)

	val replaced_ids = setOf(manifest.latest.release, manifest.latest.snapshot)

	val other_options = manifest.versions
		.filter { version -> version.id !in replaced_ids }
		.sortedByDescending { version -> version.release_time }
		.map { version -> DropdownOption(version.id, version.id) }

	return latest_options + other_options
}