package quantum.client.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import quantum.shared.GameVersions
import quantum.shared.VersionManifest
import quantum.shared.VersionType

data class VersionChoice(
	val value: String,
	val version_id: String,
	val title: String
)

@Composable
fun VersionStep(
	manifest: VersionManifest,
	selected_version: String,
	search_text: String,
	type_filter: VersionType?,
	onSearchChange: (String) -> Unit,
	onTypeFilterChange: (VersionType?) -> Unit,
	onVersionSelected: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	val grid_state = rememberLazyGridState()
	val visible_keys by remember(grid_state) {
		derivedStateOf { grid_state.layoutInfo.visibleItemsInfo.map { item_info -> item_info.key }.toSet() }
	}
	val version_choices = remember(manifest, search_text, type_filter) { versionChoices(manifest, search_text, type_filter) }

	Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
		Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
			QuantumTextField(label = "Search versions", value = search_text, onValueChange = onSearchChange, width = 260.dp)

			QuantumTab(text = "All", is_selected = type_filter == null, onClick = { onTypeFilterChange(null) })

			VersionType.entries.forEach { version_type ->
				QuantumTab(
					text = version_type.display_name,
					is_selected = type_filter == version_type,
					onClick = { onTypeFilterChange(version_type) }
				)
			}
		}

		if(version_choices.isEmpty()) {
			BasicText(text = "No versions match `$search_text`.", style = QuantumTextStyles.label)
		} else {
			Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
				LazyVerticalGrid(
					columns = GridCells.Adaptive(minSize = 190.dp),
					state = grid_state,
					modifier = Modifier.fillMaxSize().padding(end = 14.dp),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalArrangement = Arrangement.spacedBy(10.dp)
				) {
					items(version_choices, key = { version_choice -> version_choice.value }) { version_choice ->
						VersionCard(
							version_id = version_choice.version_id,
							title = version_choice.title,
							is_selected = version_choice.value == selected_version,
							is_visible = version_choice.value in visible_keys,
							onClick = { onVersionSelected(version_choice.value) }
						)
					}
				}

				Box(modifier = Modifier.matchParentSize()) {
					VerticalScrollbar(
						adapter = rememberScrollbarAdapter(grid_state),
						modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
					)
				}
			}
		}
	}
}

private fun versionChoices(manifest: VersionManifest, search_text: String, type_filter: VersionType?): List<VersionChoice> {
	val trimmed_search = search_text.trim()

	fun matches(version_id: String, version_type: VersionType?): Boolean {
		val type_matches = type_filter == null || version_type == type_filter
		val search_matches = trimmed_search.isEmpty() || version_id.contains(trimmed_search, ignoreCase = true)
		return type_matches && search_matches
	}

	val latest_choices = mutableListOf<VersionChoice>()

	if(matches(manifest.latest.release, VersionType.RELEASE)) {
		latest_choices.add(VersionChoice(GameVersions.latest_release, manifest.latest.release, "Latest Release (${manifest.latest.release})"))
	}

	if(matches(manifest.latest.snapshot, VersionType.SNAPSHOT)) {
		latest_choices.add(VersionChoice(GameVersions.latest_snapshot, manifest.latest.snapshot, "Latest Snapshot (${manifest.latest.snapshot})"))
	}

	val replaced_ids = latest_choices.map { version_choice -> version_choice.version_id }.toSet()

	val other_choices = manifest.versions
		.filter { version -> matches(version.id, VersionType.fromApiName(version.type)) }
		.filter { version -> version.id !in replaced_ids }
		.sortedByDescending { version -> version.release_time }
		.map { version -> VersionChoice(version.id, version.id, version.id) }

	return latest_choices + other_choices
}