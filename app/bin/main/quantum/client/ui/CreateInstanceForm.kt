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
import quantum.shared.FabricLoaderInfo
import quantum.shared.GameVersions
import quantum.shared.GroupStore
import quantum.shared.Instance
import quantum.shared.InstanceStore
import quantum.shared.Loader
import quantum.shared.LoaderVersionStore
import quantum.shared.NameRules
import quantum.shared.VersionManifest
import quantum.shared.VersionStore
import quantum.shared.VersionType

@Composable
fun CreateInstanceForm(edition: Edition, onCreated: () -> Unit, onCancel: () -> Unit) {
	val groups = remember(edition) { GroupStore.loadForEdition(edition) }
	val group_options = listOf(DropdownOption<String?>(null, "No group")) + groups.map { group -> DropdownOption<String?>(group.id, group.name) }
	val type_options = listOf(DropdownOption<VersionType?>(null, "All")) + VersionType.entries.map { version_type -> DropdownOption<VersionType?>(version_type, version_type.display_name) }

	var instance_name by remember { mutableStateOf("") }
	var game_version by remember { mutableStateOf(if(edition == Edition.JAVA) GameVersions.latest_release else "latest") }
	var selected_loader by remember { mutableStateOf(Loader.VANILLA) }
	var selected_group_id by remember { mutableStateOf<String?>(null) }
	var error_message by remember { mutableStateOf<String?>(null) }

	var manifest by remember { mutableStateOf<VersionManifest?>(null) }
	var version_load_failed by remember { mutableStateOf(false) }
	var retry_count by remember { mutableIntStateOf(0) }
	var version_type_filter by remember { mutableStateOf<VersionType?>(null) }

	var fabric_versions by remember { mutableStateOf<List<FabricLoaderInfo>?>(null) }
	var fabric_load_failed by remember { mutableStateOf(false) }
	var fabric_retry_count by remember { mutableIntStateOf(0) }
	var selected_loader_version by remember { mutableStateOf<String?>(null) }

	val loaded_manifest = manifest
	val version_options = remember(loaded_manifest, version_type_filter) {
		if(loaded_manifest != null) versionOptions(loaded_manifest, version_type_filter) else emptyList()
	}
	val resolved_game_version = if(loaded_manifest != null) GameVersions.resolve(game_version, loaded_manifest) else null

	val loaded_fabric_versions = fabric_versions
	val fabric_options = remember(loaded_fabric_versions) {
		loaded_fabric_versions.orEmpty().map { fabric_version ->
			val label = if(fabric_version.stable) fabric_version.version else "${fabric_version.version} (unstable)"
			DropdownOption<String?>(fabric_version.version, label)
		}
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

	LaunchedEffect(selected_loader, resolved_game_version, fabric_retry_count) {
		fabric_versions = null
		fabric_load_failed = false
		selected_loader_version = null

		if(selected_loader == Loader.FABRIC && resolved_game_version != null) {
			try {
				val fetched_versions = withContext(Dispatchers.IO) { LoaderVersionStore.fetchFabricVersions(resolved_game_version) }
				fabric_versions = fetched_versions

				val default_version = fetched_versions.firstOrNull { fabric_version -> fabric_version.stable } ?: fetched_versions.firstOrNull()
				selected_loader_version = default_version?.version
			} catch(error: CancellationException) {
				throw error
			} catch(error: Exception) {
				fabric_load_failed = true
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
				Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
					QuantumDropdown(
						label = "Game version",
						options = version_options,
						selected_value = game_version,
						onValueSelected = { version_id ->
							game_version = version_id
							error_message = null
						}
					)

					QuantumDropdown(
						label = "Type",
						options = type_options,
						selected_value = version_type_filter,
						width = 160.dp,
						onValueSelected = { version_type ->
							version_type_filter = version_type
							val filtered_options = versionOptions(loaded_manifest, version_type)

							if(filtered_options.none { option -> option.value == game_version }) {
								game_version = filtered_options.firstOrNull()?.value ?: GameVersions.latest_release
							}
						}
					)
				}
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
							onClick = {
								selected_loader = loader
								error_message = null
							}
						)
					}
				}
			}
		}

		if(edition == Edition.JAVA && selected_loader == Loader.FABRIC) {
			if(loaded_fabric_versions != null && loaded_fabric_versions.isNotEmpty()) {
				QuantumDropdown(
					label = "Fabric version",
					options = fabric_options,
					selected_value = selected_loader_version,
					onValueSelected = { fabric_version -> selected_loader_version = fabric_version }
				)
			} else {
				Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
					BasicText(text = "Fabric version", style = QuantumTextStyles.label)

					if(fabric_load_failed) {
						Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
							BasicText(text = "Could not load the Fabric versions. Check your internet connection.", style = QuantumTextStyles.error)
							QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = { fabric_retry_count += 1 })
						}
					} else if(loaded_fabric_versions != null) {
						BasicText(text = "Fabric is not available for `$resolved_game_version`.", style = QuantumTextStyles.error)
					} else if(resolved_game_version == null) {
						BasicText(text = "Waiting for the version list…", style = QuantumTextStyles.body)
					} else {
						BasicText(text = "Loading the Fabric versions…", style = QuantumTextStyles.body)
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
					val is_fabric = edition == Edition.JAVA && selected_loader == Loader.FABRIC

					if(name_problem != null) {
						error_message = name_problem
					} else if(edition == Edition.JAVA && manifest == null) {
						error_message = "Wait for the version list to load before creating the instance."
					} else if(game_version.isBlank()) {
						error_message = "Enter a game version, or `latest`."
					} else if(is_fabric && fabric_load_failed) {
						error_message = "Could not load the Fabric versions. Select `Retry` and try again."
					} else if(is_fabric && fabric_versions == null) {
						error_message = "Wait for the Fabric versions to load before creating the instance."
					} else if(is_fabric && selected_loader_version == null) {
						error_message = "Fabric is not available for `$resolved_game_version`. Choose a different game version or mod loader."
					} else {
						val new_instance = Instance(
							name = final_name,
							edition = edition,
							game_version = game_version.trim(),
							loader = selected_loader,
							loader_version = if(is_fabric) selected_loader_version else null,
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

private fun versionOptions(manifest: VersionManifest, type_filter: VersionType?): List<DropdownOption<String>> {
	val show_latest_release = type_filter == null || type_filter == VersionType.RELEASE
	val show_latest_snapshot = type_filter == null || type_filter == VersionType.SNAPSHOT

	val latest_options = mutableListOf<DropdownOption<String>>()
	val replaced_ids = mutableSetOf<String>()

	if(show_latest_release) {
		latest_options.add(DropdownOption(GameVersions.latest_release, "Latest Release (${manifest.latest.release})"))
		replaced_ids.add(manifest.latest.release)
	}

	if(show_latest_snapshot) {
		latest_options.add(DropdownOption(GameVersions.latest_snapshot, "Latest Snapshot (${manifest.latest.snapshot})"))
		replaced_ids.add(manifest.latest.snapshot)
	}

	val other_options = manifest.versions
		.filter { version -> type_filter == null || VersionType.fromApiName(version.type) == type_filter }
		.filter { version -> version.id !in replaced_ids }
		.sortedByDescending { version -> version.release_time }
		.map { version -> DropdownOption(version.id, version.id) }

	return latest_options + other_options
}