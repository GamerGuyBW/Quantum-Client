package quantum.client.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import quantum.shared.Addon
import quantum.shared.Loader
import quantum.shared.LoaderGenerations
import quantum.shared.LoaderVersion
import quantum.shared.LoaderVersionStore
import quantum.shared.VersionManifest

@Composable
fun ExtrasStep(
	manifest: VersionManifest,
	game_version_id: String,
	selected_loader: Loader,
	support_data: SupportData,
	generations: LoaderGenerations?,
	generations_failed: Boolean,
	selected_generation: Int?,
	onGenerationSelected: (Int?) -> Unit,
	onRetryGenerations: () -> Unit,
	loader_versions: List<LoaderVersion>?,
	loader_versions_failed: Boolean,
	selected_loader_version: String?,
	onLoaderVersionSelected: (String?) -> Unit,
	onRetryLoaderVersions: () -> Unit,
	checked_addons: Set<Addon>,
	onAddonCheckedChange: (Addon, Boolean) -> Unit,
	onAddonVersionChange: (Addon, LoaderVersion?, Boolean) -> Unit,
	onRetryAddonSupport: (Addon) -> Unit,
	modifier: Modifier = Modifier
) {
	val scroll_state = rememberScrollState()
	val loader_name = selected_loader.full_name
	val all_addon_support_loaded = Addon.entries.all { addon -> addon in support_data.addon_support }
	val has_compatible_addon = Addon.entries.any { addon ->
		support_data.addon_support[addon]?.get(selected_loader)?.contains(game_version_id) == true
	}

	Box(modifier = modifier) {
		Column(
			modifier = Modifier.fillMaxWidth().verticalScroll(scroll_state).padding(end = 14.dp),
			verticalArrangement = Arrangement.spacedBy(18.dp)
		) {
			if(LoaderVersionStore.hasVersionList(selected_loader)) {
				if(LoaderVersionStore.hasGenerations(selected_loader)) {
					Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
						if(generations != null && generations.available.isNotEmpty()) {
							val generation_options = generations.available.map { generation ->
								val label = if(generation == generations.stable) "Gen $generation (stable)" else "Gen $generation"
								DropdownOption<Int?>(generation, label)
							}

							QuantumDropdown(
								label = "Generation",
								options = generation_options,
								selected_value = selected_generation,
								onValueSelected = onGenerationSelected
							)
						} else {
							BasicText(text = "Generation", style = QuantumTextStyles.label)

							if(generations_failed) {
								Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
									BasicText(text = "Could not load the $loader_name generations. Check your internet connection.", style = QuantumTextStyles.error)
									QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = onRetryGenerations)
								}
							} else if(generations != null) {
								BasicText(text = "$loader_name has no generation for `$game_version_id`.", style = QuantumTextStyles.error)
							} else {
								BasicText(text = "Loading the $loader_name generations…", style = QuantumTextStyles.body)
							}
						}
					}
				}

				if(loader_versions != null && loader_versions.isNotEmpty()) {
					QuantumDropdown(
						label = "$loader_name version",
						options = loaderVersionOptions(loader_versions),
						selected_value = selected_loader_version,
						onValueSelected = onLoaderVersionSelected
					)
				} else {
					Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
						BasicText(text = "$loader_name version", style = QuantumTextStyles.label)

						if(loader_versions_failed) {
							Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
								BasicText(text = "Could not load the $loader_name versions. Check your internet connection.", style = QuantumTextStyles.error)
								QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = onRetryLoaderVersions)
							}
						} else if(loader_versions != null) {
							BasicText(text = "$loader_name is not available for `$game_version_id`.", style = QuantumTextStyles.error)
						} else {
							BasicText(text = "Loading the $loader_name versions…", style = QuantumTextStyles.body)
						}
					}
				}
			}

			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				BasicText(text = "Add-ons", style = QuantumTextStyles.label)

				if(all_addon_support_loaded && !has_compatible_addon) {
					BasicText(text = "No add-ons are compatible with `$game_version_id` and $loader_name.", style = QuantumTextStyles.body)
				}

				Addon.entries.forEach { addon ->
					key(addon) {
						AddonRow(
							addon = addon,
							manifest = manifest,
							addon_support = support_data.addon_support[addon],
							is_support_failed = addon in support_data.failed_addons,
							selected_loader = selected_loader,
							game_version_id = game_version_id,
							is_checked = addon in checked_addons,
							onCheckedChange = { is_checked -> onAddonCheckedChange(addon, is_checked) },
							onVersionChange = { addon_version, is_failed -> onAddonVersionChange(addon, addon_version, is_failed) },
							onRetrySupport = { onRetryAddonSupport(addon) }
						)
					}
				}
			}
		}

		VerticalScrollbar(
			adapter = rememberScrollbarAdapter(scroll_state),
			modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
		)
	}
}