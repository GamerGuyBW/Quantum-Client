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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import quantum.shared.Addon
import quantum.shared.AddonVersionStore
import quantum.shared.Loader
import quantum.shared.LoaderVersion
import quantum.shared.VersionManifest
import quantum.shared.VersionRanges

@Composable
fun AddonRow(
	addon: Addon,
	manifest: VersionManifest,
	addon_support: Map<Loader, List<String>>?,
	is_support_failed: Boolean,
	selected_loader: Loader,
	game_version_id: String,
	is_checked: Boolean,
	onCheckedChange: (Boolean) -> Unit,
	onVersionChange: (LoaderVersion?, Boolean) -> Unit,
	onRetrySupport: () -> Unit
) {
	val latest_on_checked_change by rememberUpdatedState(onCheckedChange)
	val latest_on_version_change by rememberUpdatedState(onVersionChange)
	val addon_name = addon.display_name

	var addon_versions by remember { mutableStateOf<List<LoaderVersion>?>(null) }
	var versions_failed by remember { mutableStateOf(false) }
	var versions_retry_count by remember { mutableIntStateOf(0) }
	var selected_version by remember { mutableStateOf<String?>(null) }

	val loader_game_versions = addon_support?.get(selected_loader)
	val supported_loaders = addon_support?.filterValues { game_versions -> game_versions.isNotEmpty() }?.keys.orEmpty()
	val supports_loader = selected_loader in supported_loaders
	val is_available = loader_game_versions != null && game_version_id in loader_game_versions
	val loaded_versions = addon_versions
	val version_options = remember(loaded_versions) { loaderVersionOptions(loaded_versions.orEmpty()) }
	val tooltip_sections = remember(addon, addon_support, manifest) {
		if(addon_support == null) {
			null
		} else if(AddonVersionStore.isUniversal(addon)) {
			listOf(SupportSection(null, listOf("Every loader and every version.")))
		} else {
			addon_support.map { (loader, game_versions) -> SupportSection(loader.full_name, VersionRanges.describe(game_versions, manifest)) }
		}
	}

	LaunchedEffect(is_available, is_checked) {
		if(!is_available && is_checked && addon_support != null) {
			latest_on_checked_change(false)
		}
	}

	LaunchedEffect(is_checked, is_available, game_version_id, versions_retry_count) {
		addon_versions = null
		versions_failed = false
		selected_version = null
		latest_on_version_change(null, false)

		if(is_checked && is_available) {
			try {
				val fetched_versions = withContext(Dispatchers.IO) { AddonVersionStore.fetchVersions(addon, game_version_id) }
				val default_version = defaultLoaderVersion(fetched_versions)
				addon_versions = fetched_versions
				selected_version = default_version?.version
				latest_on_version_change(default_version, false)
			} catch(error: CancellationException) {
				throw error
			} catch(error: Exception) {
				versions_failed = true
				latest_on_version_change(null, true)
			}
		}
	}

	Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
		Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
			SupportTooltip(title = addon_name, sections = tooltip_sections, is_failed = is_support_failed) {
				QuantumCheckbox(
					text = addon_name,
					is_checked = is_checked,
					is_enabled = is_available,
					onCheckedChange = { new_value -> onCheckedChange(new_value) }
				)
			}

			if(is_checked && is_available) {
				if(loaded_versions != null && loaded_versions.isNotEmpty()) {
					QuantumDropdown(
						options = version_options,
						selected_value = selected_version,
						width = 260.dp,
						onValueSelected = { version_id ->
							selected_version = version_id
							onVersionChange(loaded_versions.firstOrNull { addon_version -> addon_version.version == version_id }, false)
						}
					)
				} else if(versions_failed) {
					Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
						BasicText(text = "Could not load the $addon_name versions. Check your internet connection.", style = QuantumTextStyles.error)
						QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = { versions_retry_count += 1 })
					}
				} else if(loaded_versions != null) {
					BasicText(text = "$addon_name is not available for `$game_version_id`.", style = QuantumTextStyles.error)
				} else {
					BasicText(text = "Loading the $addon_name versions…", style = QuantumTextStyles.body)
				}
			}
		}

		if(!is_available) {
			val reason_modifier = Modifier.padding(start = 28.dp)

			if(is_support_failed && addon_support == null) {
				Row(modifier = reason_modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
					BasicText(text = "Could not load the $addon_name version list. Check your internet connection.", style = QuantumTextStyles.error)
					QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = onRetrySupport)
				}
			} else if(addon_support == null) {
				BasicText(text = "Loading the $addon_name version list…", style = QuantumTextStyles.label, modifier = reason_modifier)
			} else if(!supports_loader) {
				val loader_names = supported_loaders.joinToString(" or ") { loader -> loader.full_name }
				BasicText(text = "Needs $loader_names.", style = QuantumTextStyles.label, modifier = reason_modifier)
			} else if(selected_loader == Loader.VANILLA) {
				BasicText(text = "Not available for `$game_version_id`.", style = QuantumTextStyles.label, modifier = reason_modifier)
			} else {
				BasicText(text = "Not available for `$game_version_id` with ${selected_loader.full_name}.", style = QuantumTextStyles.label, modifier = reason_modifier)
			}
		}
	}
}