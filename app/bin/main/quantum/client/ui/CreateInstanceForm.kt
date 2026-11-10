package quantum.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import quantum.shared.Edition
import quantum.shared.GameVersions
import quantum.shared.GroupStore
import quantum.shared.Instance
import quantum.shared.InstanceStore
import quantum.shared.Loader
import quantum.shared.LoaderPreloader
import quantum.shared.LoaderVersion
import quantum.shared.LoaderVersionStore
import quantum.shared.NameRules
import quantum.shared.PreloadState
import quantum.shared.PreloadStatus
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

	var supported_game_versions by remember { mutableStateOf<Set<String>?>(null) }
	var supported_ready_for by remember { mutableStateOf<Loader?>(null) }
	var supported_status by remember { mutableStateOf<PreloadStatus?>(null) }

	var loader_versions by remember { mutableStateOf<List<LoaderVersion>?>(null) }
	var loader_load_failed by remember { mutableStateOf(false) }
	var loader_retry_count by remember { mutableIntStateOf(0) }
	var selected_loader_version by remember { mutableStateOf<String?>(null) }

	val loaded_manifest = manifest
	val is_version_list_ready = loaded_manifest != null && supported_ready_for == selected_loader
	val version_options = remember(loaded_manifest, version_type_filter, supported_game_versions, is_version_list_ready) {
		if(loaded_manifest != null && is_version_list_ready) versionOptions(loaded_manifest, version_type_filter, supported_game_versions) else emptyList()
	}
	val resolved_game_version = if(loaded_manifest != null) GameVersions.resolve(game_version, loaded_manifest) else null

	val needs_loader_version = edition == Edition.JAVA && LoaderVersionStore.hasVersionList(selected_loader)
	val loader_name = selected_loader.display_name
	val loaded_loader_versions = loader_versions
	val loader_version_options = remember(loaded_loader_versions) {
		loaded_loader_versions.orEmpty().map { loader_version ->
			val label = if(loader_version.is_stable) loader_version.display_name else "${loader_version.display_name} (unstable)"
			DropdownOption<String?>(loader_version.version, label)
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

	LaunchedEffect(edition, selected_loader) {
		supported_game_versions = null
		supported_ready_for = null
		supported_status = null

		if(edition == Edition.JAVA) {
			if(!LoaderVersionStore.hasVersionList(selected_loader)) {
				supported_ready_for = selected_loader
			} else {
				LoaderPreloader.prioritise(selected_loader)

				while(supported_ready_for != selected_loader) {
					val current_status = LoaderPreloader.status(selected_loader)
					supported_status = current_status

					if(current_status.state == PreloadState.READY) {
						supported_game_versions = withContext(Dispatchers.IO) { LoaderVersionStore.loadSupportedGameVersions(selected_loader) }
						supported_ready_for = selected_loader
					} else {
						delay(250)
					}
				}
			}
		}
	}

	LaunchedEffect(version_options) {
		if(version_options.isNotEmpty() && version_options.none { option -> option.value == game_version }) {
			game_version = version_options.first().value
		}
	}

	LaunchedEffect(selected_loader, resolved_game_version, supported_ready_for, loader_retry_count) {
		loader_versions = null
		loader_load_failed = false
		selected_loader_version = null

		if(needs_loader_version && supported_ready_for == selected_loader && resolved_game_version != null) {
			try {
				val fetched_versions = withContext(Dispatchers.IO) { LoaderVersionStore.fetchVersions(selected_loader, resolved_game_version) }
				loader_versions = fetched_versions

				val default_version = fetched_versions.firstOrNull { loader_version -> loader_version.is_stable } ?: fetched_versions.firstOrNull()
				selected_loader_version = default_version?.version
			} catch(error: CancellationException) {
				throw error
			} catch(error: Exception) {
				loader_load_failed = true
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

			if(is_version_list_ready) {
				Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
					if(version_options.isEmpty()) {
						val type_text = version_type_filter?.display_name?.let { type_name -> "$type_name " } ?: ""

						Column(modifier = Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
							BasicText(text = "Game version", style = QuantumTextStyles.label)
							BasicText(text = "No ${type_text}versions support $loader_name.", style = QuantumTextStyles.error)
						}
					} else {
						QuantumDropdown(
							label = "Game version",
							options = version_options,
							selected_value = game_version,
							onValueSelected = { version_id ->
								game_version = version_id
								error_message = null
							}
						)
					}

					QuantumDropdown(
						label = "Type",
						options = type_options,
						selected_value = version_type_filter,
						width = 160.dp,
						onValueSelected = { version_type -> version_type_filter = version_type }
					)
				}
			} else {
				Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
					BasicText(text = "Game version", style = QuantumTextStyles.label)

					val current_status = supported_status

					if(version_load_failed) {
						Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
							BasicText(text = "Could not load the version list. Check your internet connection.", style = QuantumTextStyles.error)
							QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = { retry_count += 1 })
						}
					} else if(current_status?.state == PreloadState.FAILED) {
						Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
							BasicText(text = "Could not load the versions supported by $loader_name. Check your internet connection.", style = QuantumTextStyles.error)
							QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = { LoaderPreloader.retry(selected_loader) })
						}
					} else if(current_status != null && current_status.total_count > 0) {
						BasicText(
							text = "Checking $loader_name versions… ${"%,d".format(current_status.checked_count)} of ${"%,d".format(current_status.total_count)}",
							style = QuantumTextStyles.body
						)
						BasicText(text = "Checks continue in the background if you close this form.", style = QuantumTextStyles.label)
					} else if(current_status?.state == PreloadState.WAITING) {
						BasicText(text = "Waiting to check $loader_name versions…", style = QuantumTextStyles.body)
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

		if(needs_loader_version && version_options.isNotEmpty()) {
			if(loaded_loader_versions != null && loaded_loader_versions.isNotEmpty()) {
				QuantumDropdown(
					label = "$loader_name version",
					options = loader_version_options,
					selected_value = selected_loader_version,
					onValueSelected = { loader_version -> selected_loader_version = loader_version }
				)
			} else {
				Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
					BasicText(text = "$loader_name version", style = QuantumTextStyles.label)

					if(loader_load_failed) {
						Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
							BasicText(text = "Could not load the $loader_name versions. Check your internet connection.", style = QuantumTextStyles.error)
							QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = { loader_retry_count += 1 })
						}
					} else if(loaded_loader_versions != null) {
						BasicText(text = "$loader_name is not available for `$resolved_game_version`.", style = QuantumTextStyles.error)
					} else {
						BasicText(text = "Loading the $loader_name versions…", style = QuantumTextStyles.body)
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
					val is_version_listed = version_options.any { option -> option.value == game_version }
					val chosen_loader_version = loaded_loader_versions?.firstOrNull { loader_version -> loader_version.version == selected_loader_version }

					if(name_problem != null) {
						error_message = name_problem
					} else if(edition == Edition.JAVA && !is_version_list_ready) {
						error_message = "Wait for the version list to load before creating the instance."
					} else if(edition == Edition.JAVA && !is_version_listed) {
						error_message = "Choose a game version that supports $loader_name."
					} else if(game_version.isBlank()) {
						error_message = "Enter a game version, or `latest`."
					} else if(needs_loader_version && loader_load_failed) {
						error_message = "Could not load the $loader_name versions. Select `Retry` and try again."
					} else if(needs_loader_version && loader_versions == null) {
						error_message = "Wait for the $loader_name versions to load before creating the instance."
					} else if(needs_loader_version && chosen_loader_version == null) {
						error_message = "$loader_name is not available for `$resolved_game_version`. Choose a different game version or mod loader."
					} else {
						val new_instance = Instance(
							name = final_name,
							edition = edition,
							game_version = game_version.trim(),
							loader = selected_loader,
							loader_version = if(needs_loader_version) chosen_loader_version?.version else null,
							loader_version_name = if(needs_loader_version) chosen_loader_version?.display_name else null,
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

private fun versionOptions(
	manifest: VersionManifest,
	type_filter: VersionType?,
	supported_ids: Set<String>?
): List<DropdownOption<String>> {
	fun isSupported(version_id: String): Boolean {
		return supported_ids == null || version_id in supported_ids
	}

	val show_latest_release = (type_filter == null || type_filter == VersionType.RELEASE) && isSupported(manifest.latest.release)
	val show_latest_snapshot = (type_filter == null || type_filter == VersionType.SNAPSHOT) && isSupported(manifest.latest.snapshot)

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
		.filter { version -> isSupported(version.id) }
		.filter { version -> version.id !in replaced_ids }
		.sortedByDescending { version -> version.release_time }
		.map { version -> DropdownOption(version.id, version.id) }

	return latest_options + other_options
}