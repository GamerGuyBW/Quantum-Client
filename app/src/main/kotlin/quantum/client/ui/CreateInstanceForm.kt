package quantum.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import quantum.shared.Addon
import quantum.shared.AddonVersionStore
import quantum.shared.Edition
import quantum.shared.GameVersions
import quantum.shared.GroupStore
import quantum.shared.Instance
import quantum.shared.InstanceAddon
import quantum.shared.InstanceStore
import quantum.shared.Loader
import quantum.shared.LoaderFamily
import quantum.shared.LoaderGenerations
import quantum.shared.LoaderVersion
import quantum.shared.LoaderVersionStore
import quantum.shared.NameRules
import quantum.shared.SetupTaskState
import quantum.shared.VersionManifest
import quantum.shared.VersionSetup
import quantum.shared.VersionStore
import quantum.shared.VersionType

enum class CreationStep {
	VERSION,
	LOADER,
	PROJECT,
	EXTRAS,
	NAME
}

@Composable
fun CreateInstanceForm(edition: Edition, onCreated: () -> Unit, onCancel: () -> Unit) {
	val groups = remember(edition) { GroupStore.loadForEdition(edition) }
	val coroutine_scope = rememberCoroutineScope()

	var current_step by remember { mutableStateOf(CreationStep.VERSION) }
	var error_message by remember { mutableStateOf<String?>(null) }
	var is_verifying by remember { mutableStateOf(false) }

	var manifest by remember { mutableStateOf<VersionManifest?>(null) }
	var manifest_failed by remember { mutableStateOf(false) }
	var manifest_retry_count by remember { mutableIntStateOf(0) }
	var game_version by remember { mutableStateOf(if(edition == Edition.JAVA) GameVersions.latest_release else "latest") }
	var search_text by remember { mutableStateOf("") }
	var version_type_filter by remember { mutableStateOf<VersionType?>(null) }

	var support_data by remember { mutableStateOf(SupportData()) }
	var selected_family by remember { mutableStateOf(LoaderFamily.VANILLA) }
	var selected_loader by remember { mutableStateOf<Loader?>(Loader.VANILLA) }

	var generations by remember { mutableStateOf<LoaderGenerations?>(null) }
	var generations_failed by remember { mutableStateOf(false) }
	var generations_retry_count by remember { mutableIntStateOf(0) }
	var selected_generation by remember { mutableStateOf<Int?>(null) }

	var loader_versions by remember { mutableStateOf<List<LoaderVersion>?>(null) }
	var loader_versions_failed by remember { mutableStateOf(false) }
	var loader_retry_count by remember { mutableIntStateOf(0) }
	var selected_loader_version by remember { mutableStateOf<String?>(null) }

	var checked_addons by remember { mutableStateOf<Set<Addon>>(emptySet()) }
	var chosen_addon_versions by remember { mutableStateOf<Map<Addon, LoaderVersion>>(emptyMap()) }
	var failed_addon_versions by remember { mutableStateOf<Set<Addon>>(emptySet()) }

	var instance_name by remember { mutableStateOf("") }
	var is_name_edited by remember { mutableStateOf(false) }
	var selected_group_id by remember { mutableStateOf<String?>(null) }

	val loaded_manifest = manifest
	val resolved_game_version = if(edition == Edition.BEDROCK) {
		game_version.trim()
	} else if(loaded_manifest != null) {
		GameVersions.resolve(game_version, loaded_manifest)
	} else {
		null
	}

	val chosen_loader = selected_loader
	val family_projects = projectsOf(selected_family)
	val steps = if(edition == Edition.BEDROCK) {
		listOf(CreationStep.VERSION, CreationStep.NAME)
	} else if(family_projects.size > 1) {
		listOf(CreationStep.VERSION, CreationStep.LOADER, CreationStep.PROJECT, CreationStep.EXTRAS, CreationStep.NAME)
	} else {
		listOf(CreationStep.VERSION, CreationStep.LOADER, CreationStep.EXTRAS, CreationStep.NAME)
	}
	val step_index = steps.indexOf(current_step).coerceAtLeast(0)
	val is_last_step = step_index == steps.lastIndex

	val needs_loader_version = edition == Edition.JAVA && chosen_loader != null && LoaderVersionStore.hasVersionList(chosen_loader)
	val is_loader_supported = chosen_loader != null && resolved_game_version != null && support_data.loaderStatus(chosen_loader, resolved_game_version) == SupportStatus.SUPPORTED
	val chosen_loader_version = loader_versions?.firstOrNull { loader_version -> loader_version.version == selected_loader_version }
	val ordered_addons = Addon.entries.filter { addon -> addon in checked_addons }

	LaunchedEffect(edition, manifest_retry_count) {
		if(edition == Edition.JAVA) {
			manifest_failed = false

			try {
				manifest = withContext(Dispatchers.IO) { VersionStore.fetchManifest() }
			} catch(error: CancellationException) {
				throw error
			} catch(error: Exception) {
				manifest_failed = true
			}
		}
	}

	LaunchedEffect(edition) {
		if(edition == Edition.JAVA) {
			while(true) {
				val current_data = support_data
				val loader_versions_map = current_data.loader_versions.toMutableMap()
				val failed_loaders = mutableSetOf<Loader>()
				val addon_support = current_data.addon_support.toMutableMap()
				val failed_addons = mutableSetOf<Addon>()

				Loader.entries
					.filter { loader -> LoaderVersionStore.hasVersionList(loader) && loader !in loader_versions_map }
					.forEach { loader ->
						val loader_state = VersionSetup.loaderState(loader)
						val loaded_versions = if(loader_state == SetupTaskState.DONE) {
							withContext(Dispatchers.IO) { LoaderVersionStore.loadSupportedGameVersions(loader) }
						} else {
							null
						}

						if(loaded_versions != null) {
							loader_versions_map[loader] = loaded_versions
						} else if(loader_state == SetupTaskState.FAILED || loader_state == SetupTaskState.DONE) {
							failed_loaders.add(loader)
						}
					}

				Addon.entries
					.filter { addon -> addon !in addon_support }
					.forEach { addon ->
						val addon_state = VersionSetup.addonState(addon)
						val loaded_support = if(addon_state == SetupTaskState.DONE) {
							withContext(Dispatchers.IO) { AddonVersionStore.loadSupport(addon) }
						} else {
							null
						}

						if(loaded_support != null) {
							addon_support[addon] = loaded_support
						} else if(addon_state == SetupTaskState.FAILED || addon_state == SetupTaskState.DONE) {
							failed_addons.add(addon)
						}
					}

				support_data = SupportData(loader_versions_map, failed_loaders, addon_support, failed_addons)

				val is_everything_loaded = Loader.entries.all { loader -> !LoaderVersionStore.hasVersionList(loader) || loader in loader_versions_map } &&
					Addon.entries.all { addon -> addon in addon_support }

				if(is_everything_loaded) {
					break
				}

				delay(250)
			}
		}
	}

	LaunchedEffect(chosen_loader, resolved_game_version, is_loader_supported, generations_retry_count) {
		generations = null
		generations_failed = false
		selected_generation = null

		if(chosen_loader != null && resolved_game_version != null && is_loader_supported && LoaderVersionStore.hasGenerations(chosen_loader)) {
			try {
				val loaded_generations = withContext(Dispatchers.IO) { LoaderVersionStore.loadGenerations(chosen_loader, resolved_game_version) }
				generations = loaded_generations

				if(loaded_generations != null) {
					selected_generation = if(loaded_generations.stable in loaded_generations.available) loaded_generations.stable else loaded_generations.available.maxOrNull()
				}
			} catch(error: CancellationException) {
				throw error
			} catch(error: Exception) {
				generations_failed = true
			}
		}
	}

	LaunchedEffect(chosen_loader, resolved_game_version, is_loader_supported, selected_generation, loader_retry_count) {
		loader_versions = null
		loader_versions_failed = false
		selected_loader_version = null

		val needs_generation = chosen_loader != null && LoaderVersionStore.hasGenerations(chosen_loader)

		if(needs_loader_version && chosen_loader != null && resolved_game_version != null && is_loader_supported && (!needs_generation || selected_generation != null)) {
			try {
				val fetched_versions = withContext(Dispatchers.IO) { LoaderVersionStore.fetchVersions(chosen_loader, resolved_game_version, selected_generation) }
				loader_versions = fetched_versions
				selected_loader_version = defaultLoaderVersion(fetched_versions)?.version
			} catch(error: CancellationException) {
				throw error
			} catch(error: Exception) {
				loader_versions_failed = true
			}
		}
	}

	fun retryFailedSupport() {
		support_data.failed_loaders.forEach { loader -> VersionSetup.retry(loader) }
	}

	fun chooseFamily(family: LoaderFamily) {
		if(family != selected_family) {
			selected_family = family
			val projects = projectsOf(family)
			selected_loader = if(projects.size == 1) projects.first() else null
		}
		error_message = null
	}

	fun suggestedName(): String {
		val version_text = resolved_game_version ?: game_version
		val suggestion = if(edition == Edition.BEDROCK) "Bedrock $version_text" else "$version_text ${selected_family.display_name}"
		return NameRules.cleanTyped(suggestion).trimEnd()
	}

	fun stepProblem(step: CreationStep): String? {
		return when(step) {
			CreationStep.VERSION -> {
				if(edition == Edition.BEDROCK && game_version.isBlank()) {
					"Enter a game version, or `latest`."
				} else if(edition == Edition.JAVA && loaded_manifest == null) {
					"Wait for the version list to load."
				} else {
					null
				}
			}
			CreationStep.LOADER -> {
				val family_status = if(resolved_game_version != null) support_data.familyStatus(selected_family, resolved_game_version) else SupportStatus.LOADING

				when(family_status) {
					SupportStatus.SUPPORTED -> null
					SupportStatus.LOADING -> "Wait for the loader lists to load."
					SupportStatus.FAILED -> "Could not load the ${selected_family.display_name} version lists. Select `Retry` and try again."
					SupportStatus.UNSUPPORTED -> "${selected_family.display_name} is not available for `$resolved_game_version`. Choose a different mod loader."
				}
			}
			CreationStep.PROJECT -> {
				if(chosen_loader == null) {
					"Choose a ${selected_family.display_name} project."
				} else if(!is_loader_supported) {
					"${chosen_loader.full_name} is not available for `$resolved_game_version`. Choose a different project."
				} else {
					null
				}
			}
			CreationStep.EXTRAS -> {
				val missing_addon = ordered_addons.firstOrNull { addon -> addon !in chosen_addon_versions }
				val loader_name = chosen_loader?.full_name ?: selected_family.display_name

				if(chosen_loader == null || !is_loader_supported) {
					"Choose a mod loader that supports `$resolved_game_version`."
				} else if(LoaderVersionStore.hasGenerations(chosen_loader) && generations_failed) {
					"Could not load the $loader_name generations. Select `Retry` and try again."
				} else if(LoaderVersionStore.hasGenerations(chosen_loader) && selected_generation == null) {
					"Wait for the $loader_name generations to load."
				} else if(needs_loader_version && loader_versions_failed) {
					"Could not load the $loader_name versions. Select `Retry` and try again."
				} else if(needs_loader_version && loader_versions == null) {
					"Wait for the $loader_name versions to load."
				} else if(needs_loader_version && chosen_loader_version == null) {
					"$loader_name is not available for `$resolved_game_version`. Choose a different game version or mod loader."
				} else if(missing_addon != null && missing_addon in failed_addon_versions) {
					"Could not load the ${missing_addon.display_name} versions. Select `Retry` and try again."
				} else if(missing_addon != null) {
					"Wait for the ${missing_addon.display_name} versions to load."
				} else {
					null
				}
			}
			CreationStep.NAME -> {
				val taken_names = InstanceStore.loadForEdition(edition).map { instance -> instance.name }
				NameRules.findProblem(instance_name.trimEnd(), "instance", edition, taken_names)
			}
		}
	}

	fun createInstance() {
		val final_loader = if(edition == Edition.JAVA) chosen_loader ?: Loader.VANILLA else Loader.VANILLA
		val checked_game_version = resolved_game_version
		val loader_version_to_check = chosen_loader_version
		val addons_to_save = if(edition == Edition.JAVA) ordered_addons else emptyList()

		val new_instance = Instance(
			name = instance_name.trimEnd(),
			edition = edition,
			game_version = game_version.trim(),
			loader = final_loader,
			loader_version = if(needs_loader_version) loader_version_to_check?.version else null,
			loader_version_name = if(needs_loader_version) loader_version_to_check?.display_name else null,
			generation = if(edition == Edition.JAVA && LoaderVersionStore.hasGenerations(final_loader)) selected_generation else null,
			group_id = selected_group_id,
			addons = addons_to_save.map { addon ->
				val addon_version = chosen_addon_versions.getValue(addon)
				InstanceAddon(addon, addon_version.version, addon_version.display_name)
			}
		)

		val is_loader_check_needed = needs_loader_version && loader_version_to_check != null && loader_version_to_check.is_verifiable && checked_game_version != null
		val is_addon_check_needed = addons_to_save.any { addon -> AddonVersionStore.needsBaseCheck(addon, final_loader) }

		if(!is_loader_check_needed && !is_addon_check_needed) {
			InstanceStore.save(new_instance)
			onCreated()
			return
		}

		is_verifying = true

		coroutine_scope.launch {
			val verification_problem = try {
				withContext(Dispatchers.IO) {
					val loader_problem = if(is_loader_check_needed && loader_version_to_check != null && checked_game_version != null) {
						LoaderVersionStore.verifyGameVersion(final_loader, loader_version_to_check, checked_game_version)
					} else {
						null
					}

					loader_problem ?: addons_to_save.firstNotNullOfOrNull { addon ->
						AddonVersionStore.verifyBase(addon, final_loader, loader_version_to_check)
					}
				}
			} catch(error: CancellationException) {
				throw error
			} catch(error: Exception) {
				"Could not confirm the chosen versions. Check your internet connection."
			}

			is_verifying = false

			if(verification_problem != null) {
				error_message = verification_problem
				loader_retry_count += 1
			} else {
				InstanceStore.save(new_instance)
				onCreated()
			}
		}
	}

	fun goNext() {
		if(is_verifying) {
			return
		}

		val problem = stepProblem(current_step)
		if(problem != null) {
			error_message = problem
			return
		}

		error_message = null

		if(is_last_step) {
			createInstance()
		} else {
			val next_step = steps[step_index + 1]
			if(next_step == CreationStep.NAME && !is_name_edited) {
				instance_name = suggestedName()
			}
			current_step = next_step
		}
	}

	fun goBack() {
		if(!is_verifying && step_index > 0) {
			current_step = steps[step_index - 1]
			error_message = null
		}
	}

	val summary_text = summaryText(
		edition = edition,
		game_version = game_version,
		resolved_game_version = resolved_game_version,
		step_index = step_index,
		steps = steps,
		selected_family = selected_family,
		chosen_loader = chosen_loader,
		chosen_loader_version = chosen_loader_version,
		selected_generation = selected_generation,
		chosen_addon_versions = ordered_addons.mapNotNull { addon -> chosen_addon_versions[addon]?.let { addon_version -> addon to addon_version } }
	)

	Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
		BasicText(text = "New ${edition.display_name} instance", style = QuantumTextStyles.label)
		BasicText(text = stepQuestion(current_step, selected_family), style = QuantumTextStyles.heading)

		val step_modifier = Modifier.weight(1f).fillMaxWidth()

		when(current_step) {
			CreationStep.VERSION -> {
				if(edition == Edition.BEDROCK) {
					Box(modifier = step_modifier) {
						QuantumTextField(
							label = "Game version",
							value = game_version,
							onValueChange = { new_value ->
								game_version = new_value
								error_message = null
							}
						)
					}
				} else if(loaded_manifest != null) {
					VersionStep(
						manifest = loaded_manifest,
						selected_version = game_version,
						search_text = search_text,
						type_filter = version_type_filter,
						onSearchChange = { new_value -> search_text = new_value },
						onTypeFilterChange = { version_type -> version_type_filter = version_type },
						onVersionSelected = { version_id ->
							game_version = version_id
							error_message = null
						},
						modifier = step_modifier
					)
				} else {
					Box(modifier = step_modifier) {
						if(manifest_failed) {
							Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
								BasicText(text = "Could not load the version list. Check your internet connection.", style = QuantumTextStyles.error)
								QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = { manifest_retry_count += 1 })
							}
						} else {
							BasicText(text = "Loading the version list…", style = QuantumTextStyles.body)
						}
					}
				}
			}
			CreationStep.LOADER -> {
				if(loaded_manifest != null && resolved_game_version != null) {
					LoaderStep(
						manifest = loaded_manifest,
						game_version_id = resolved_game_version,
						support_data = support_data,
						selected_family = selected_family,
						onFamilySelected = { family -> chooseFamily(family) },
						onRetry = { retryFailedSupport() },
						modifier = step_modifier
					)
				}
			}
			CreationStep.PROJECT -> {
				if(loaded_manifest != null && resolved_game_version != null) {
					ProjectStep(
						manifest = loaded_manifest,
						game_version_id = resolved_game_version,
						support_data = support_data,
						family = selected_family,
						selected_loader = chosen_loader,
						onLoaderSelected = { loader ->
							selected_loader = loader
							error_message = null
						},
						onRetry = { retryFailedSupport() },
						modifier = step_modifier
					)
				}
			}
			CreationStep.EXTRAS -> {
				if(loaded_manifest != null && resolved_game_version != null && chosen_loader != null) {
					ExtrasStep(
						manifest = loaded_manifest,
						game_version_id = resolved_game_version,
						selected_loader = chosen_loader,
						support_data = support_data,
						generations = generations,
						generations_failed = generations_failed,
						selected_generation = selected_generation,
						onGenerationSelected = { generation -> selected_generation = generation },
						onRetryGenerations = { generations_retry_count += 1 },
						loader_versions = loader_versions,
						loader_versions_failed = loader_versions_failed,
						selected_loader_version = selected_loader_version,
						onLoaderVersionSelected = { loader_version -> selected_loader_version = loader_version },
						onRetryLoaderVersions = { loader_retry_count += 1 },
						checked_addons = checked_addons,
						onAddonCheckedChange = { addon, is_checked ->
							checked_addons = if(is_checked) checked_addons + addon else checked_addons - addon
							error_message = null
						},
						onAddonVersionChange = { addon, addon_version, is_failed ->
							chosen_addon_versions = if(addon_version != null) chosen_addon_versions + (addon to addon_version) else chosen_addon_versions - addon
							failed_addon_versions = if(is_failed) failed_addon_versions + addon else failed_addon_versions - addon
						},
						onRetryAddonSupport = { addon -> VersionSetup.retryAddon(addon) },
						modifier = step_modifier
					)
				}
			}
			CreationStep.NAME -> {
				NameStep(
					instance_name = instance_name,
					onNameChange = { new_value ->
						instance_name = NameRules.cleanTyped(new_value)
						is_name_edited = true
						error_message = null
					},
					groups = groups,
					selected_group_id = selected_group_id,
					onGroupSelected = { group_id -> selected_group_id = group_id },
					modifier = step_modifier
				)
			}
		}

		val current_error = error_message
		if(current_error != null) {
			BasicText(text = current_error, style = QuantumTextStyles.error)
		}

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(10.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			QuantumButton(text = "Cancel", style = ButtonStyle.SECONDARY, onClick = { if(!is_verifying) onCancel() })

			BasicText(
				text = summary_text,
				style = QuantumTextStyles.label,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.weight(1f)
			)

			if(step_index > 0) {
				QuantumButton(text = "Back", style = ButtonStyle.SECONDARY, onClick = { goBack() })
			}

			QuantumButton(
				text = if(is_verifying) "Checking…" else if(is_last_step) "Create" else "Next",
				onClick = { goNext() }
			)
		}
	}
}

private fun stepQuestion(step: CreationStep, family: LoaderFamily): String {
	return when(step) {
		CreationStep.VERSION -> "Which Minecraft version?"
		CreationStep.LOADER -> "Which mod loader?"
		CreationStep.PROJECT -> "Which ${family.display_name} project? Mods made for one project do not work with the others."
		CreationStep.EXTRAS -> "Choose the loader version and add-ons."
		CreationStep.NAME -> "Name your instance."
	}
}

private fun summaryText(
	edition: Edition,
	game_version: String,
	resolved_game_version: String?,
	step_index: Int,
	steps: List<CreationStep>,
	selected_family: LoaderFamily,
	chosen_loader: Loader?,
	chosen_loader_version: LoaderVersion?,
	selected_generation: Int?,
	chosen_addon_versions: List<Pair<Addon, LoaderVersion>>
): String {
	val version_name = GameVersions.displayName(game_version.trim())
	val version_text = if(resolved_game_version != null && resolved_game_version != version_name) "$version_name ($resolved_game_version)" else version_name
	val parts = mutableListOf(version_text)

	if(edition == Edition.JAVA && step_index > steps.indexOf(CreationStep.LOADER)) {
		val loader_text = chosen_loader?.full_name ?: selected_family.display_name
		parts.add(if(chosen_loader_version != null) "$loader_text ${chosen_loader_version.display_name}" else loader_text)

		if(selected_generation != null && chosen_loader != null && LoaderVersionStore.hasGenerations(chosen_loader)) {
			parts.add("Gen $selected_generation")
		}

		chosen_addon_versions.forEach { (addon, addon_version) ->
			parts.add("+ ${addon.display_name} ${addon_version.display_name}")
		}
	}

	return parts.joinToString(" · ")
}