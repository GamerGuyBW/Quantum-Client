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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import quantum.shared.Addon
import quantum.shared.AddonVersionStore
import quantum.shared.Loader
import quantum.shared.LoaderVersion
import quantum.shared.SetupTaskState
import quantum.shared.VersionSetup

@Composable
fun AddonRow(
	addon: Addon,
	selected_loader: Loader,
	game_version_id: String,
	is_checked: Boolean,
	onCheckedChange: (Boolean) -> Unit,
	onVersionChange: (LoaderVersion?, Boolean) -> Unit
) {
	val latest_on_checked_change by rememberUpdatedState(onCheckedChange)
	val latest_on_version_change by rememberUpdatedState(onVersionChange)
	val addon_name = addon.display_name

	var supported_game_versions by remember(addon) { mutableStateOf<Set<String>?>(null) }
	var supported_failed by remember(addon) { mutableStateOf(false) }

	var addon_versions by remember { mutableStateOf<List<LoaderVersion>?>(null) }
	var versions_failed by remember { mutableStateOf(false) }
	var versions_retry_count by remember { mutableIntStateOf(0) }
	var selected_version by remember { mutableStateOf<String?>(null) }

	val loaded_supported = supported_game_versions
	val supports_loader = selected_loader in addon.base_loaders
	val is_available = supports_loader && loaded_supported != null && game_version_id in loaded_supported
	val loaded_versions = addon_versions
	val version_options = remember(loaded_versions) { loaderVersionOptions(loaded_versions.orEmpty()) }

	LaunchedEffect(addon) {
		while(supported_game_versions == null) {
			val addon_state = VersionSetup.addonState(addon)
			val loaded_game_versions = if(addon_state == SetupTaskState.DONE) {
				withContext(Dispatchers.IO) { AddonVersionStore.loadSupportedGameVersions(addon) }
			} else {
				null
			}

			if(loaded_game_versions != null) {
				supported_game_versions = loaded_game_versions
				supported_failed = false
			} else {
				supported_failed = addon_state == SetupTaskState.FAILED || addon_state == SetupTaskState.DONE
				delay(250)
			}
		}
	}

	LaunchedEffect(is_available, is_checked) {
		if(!is_available && is_checked && loaded_supported != null) {
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
			QuantumCheckbox(
				text = addon_name,
				is_checked = is_checked,
				is_enabled = is_available,
				onCheckedChange = { new_value -> onCheckedChange(new_value) }
			)

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

			if(!supports_loader) {
				val loader_names = addon.base_loaders.joinToString(" or ") { loader -> loader.display_name }
				BasicText(text = "Needs $loader_names.", style = QuantumTextStyles.label, modifier = reason_modifier)
			} else if(supported_failed) {
				Row(modifier = reason_modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
					BasicText(text = "Could not load the $addon_name version list. Check your internet connection.", style = QuantumTextStyles.error)
					QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = { VersionSetup.retryAddon(addon) })
				}
			} else if(loaded_supported == null) {
				BasicText(text = "Loading the $addon_name version list…", style = QuantumTextStyles.label, modifier = reason_modifier)
			} else {
				BasicText(text = "Not available for `$game_version_id`.", style = QuantumTextStyles.label, modifier = reason_modifier)
			}
		}
	}
}