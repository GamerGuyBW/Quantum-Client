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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import quantum.shared.Loader
import quantum.shared.LoaderFamily
import quantum.shared.VersionManifest

data class ChoiceItem<T>(
	val value: T,
	val title: String,
	val status: SupportStatus,
	val tooltip_sections: List<SupportSection>?
)

@Composable
fun LoaderStep(
	manifest: VersionManifest,
	game_version_id: String,
	support_data: SupportData,
	selected_family: LoaderFamily?,
	onFamilySelected: (LoaderFamily) -> Unit,
	onRetry: () -> Unit,
	modifier: Modifier = Modifier
) {
	val family_items = LoaderFamily.entries.map { family ->
		val projects = projectsOf(family)
		val project_ranges = projects.map { loader -> loader to support_data.loaderRanges(loader, manifest) }
		val tooltip_sections = if(project_ranges.any { (_, ranges) -> ranges == null }) {
			null
		} else {
			project_ranges.map { (loader, ranges) ->
				SupportSection(if(projects.size > 1) loader.display_name else null, ranges.orEmpty())
			}
		}

		ChoiceItem(family, family.display_name, support_data.familyStatus(family, game_version_id), tooltip_sections)
	}

	ChoiceGrid(
		choice_items = family_items,
		selected_value = selected_family,
		game_version_id = game_version_id,
		onSelected = onFamilySelected,
		onRetry = onRetry,
		modifier = modifier
	)
}

@Composable
fun ProjectStep(
	manifest: VersionManifest,
	game_version_id: String,
	support_data: SupportData,
	family: LoaderFamily,
	selected_loader: Loader?,
	onLoaderSelected: (Loader) -> Unit,
	onRetry: () -> Unit,
	modifier: Modifier = Modifier
) {
	val project_items = projectsOf(family).map { loader ->
		val ranges = support_data.loaderRanges(loader, manifest)
		ChoiceItem(loader, loader.display_name, support_data.loaderStatus(loader, game_version_id), ranges?.let { loader_ranges -> listOf(SupportSection(null, loader_ranges)) })
	}

	ChoiceGrid(
		choice_items = project_items,
		selected_value = selected_loader,
		game_version_id = game_version_id,
		onSelected = onLoaderSelected,
		onRetry = onRetry,
		modifier = modifier
	)
}

@Composable
private fun <T> ChoiceGrid(
	choice_items: List<ChoiceItem<T>>,
	selected_value: T?,
	game_version_id: String,
	onSelected: (T) -> Unit,
	onRetry: () -> Unit,
	modifier: Modifier = Modifier
) {
	val grid_state = rememberLazyGridState()
	val has_failures = choice_items.any { choice_item -> choice_item.status == SupportStatus.FAILED }

	Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
		if(has_failures) {
			Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
				BasicText(text = "Some version lists could not be loaded. Check your internet connection.", style = QuantumTextStyles.error)
				QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = onRetry)
			}
		}

		Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
			LazyVerticalGrid(
				columns = GridCells.Adaptive(minSize = 200.dp),
				state = grid_state,
				modifier = Modifier.fillMaxSize().padding(end = 14.dp),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
				verticalArrangement = Arrangement.spacedBy(10.dp)
			) {
				items(choice_items, key = { choice_item -> choice_item.title + choice_item.value.toString() }) { choice_item ->
					SupportTooltip(
						title = choice_item.title,
						sections = choice_item.tooltip_sections,
						is_failed = choice_item.status == SupportStatus.FAILED,
						modifier = Modifier.fillMaxWidth()
					) {
						ChoiceCard(
							title = choice_item.title,
							subtitle = statusText(choice_item.status, game_version_id),
							is_selected = choice_item.value == selected_value,
							is_enabled = choice_item.status == SupportStatus.SUPPORTED,
							onClick = { onSelected(choice_item.value) }
						)
					}
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