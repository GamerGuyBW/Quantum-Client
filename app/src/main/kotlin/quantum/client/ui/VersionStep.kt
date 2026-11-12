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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import quantum.shared.GameVersions
import quantum.shared.VersionManifest
import quantum.shared.VersionPreviewStore
import quantum.shared.VersionType
import quantum.shared.WikiVersionDetails

private const val scroll_settle_millis = 120L
private const val nearby_rows = 2

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
	val visible_version_ids by remember(version_choices) {
		derivedStateOf {
			version_choices
				.filter { version_choice -> version_choice.value in visible_keys }
				.map { version_choice -> version_choice.version_id }
				.distinct()
		}
	}
	val nearby_version_ids by remember(version_choices) {
		derivedStateOf {
			val visible_items = grid_state.layoutInfo.visibleItemsInfo

			if(visible_items.isEmpty() || version_choices.isEmpty()) {
				emptyList()
			} else {
				val column_count = visible_items.maxOf { item_info -> item_info.column } + 1
				val first_index = (visible_items.minOf { item_info -> item_info.index } - column_count * nearby_rows).coerceIn(0, version_choices.lastIndex)
				val last_index = (visible_items.maxOf { item_info -> item_info.index } + column_count * nearby_rows).coerceIn(first_index, version_choices.lastIndex)

				version_choices.subList(first_index, last_index + 1).map { version_choice -> version_choice.version_id }.distinct()
			}
		}
	}

	var mojang_image_urls by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
	var wiki_details by remember { mutableStateOf<Map<String, WikiVersionDetails?>>(emptyMap()) }
	var wiki_image_urls by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
	var thumbnails by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

	LaunchedEffect(nearby_version_ids) {
		val nearby_set = nearby_version_ids.toSet()
		mojang_image_urls = mojang_image_urls.filterKeys { version_id -> version_id in nearby_set }
		wiki_details = wiki_details.filterKeys { version_id -> version_id in nearby_set }

		val nearby_image_files = wiki_details.values.mapNotNull { details -> details?.image_file }.toSet()
		wiki_image_urls = wiki_image_urls.filterKeys { image_file -> image_file in nearby_image_files }

		val nearby_image_urls = nearby_version_ids.mapNotNull { version_id -> cardPreview(version_id, mojang_image_urls, wiki_details, wiki_image_urls)?.image_url }.toSet()
		thumbnails = thumbnails.filterKeys { image_url -> image_url in nearby_image_urls }

		if(nearby_version_ids.isEmpty()) {
			return@LaunchedEffect
		}

		val unknown_mojang_ids = nearby_version_ids.filter { version_id -> version_id !in mojang_image_urls }
		if(unknown_mojang_ids.isNotEmpty()) {
			loadInterruptibly { VersionPreviewStore.patchNoteImageUrls(unknown_mojang_ids, allow_download = false) }.onSuccess { image_urls ->
				mojang_image_urls = mojang_image_urls + image_urls
			}
		}

		val unknown_wiki_ids = nearby_version_ids.filter { version_id -> version_id !in wiki_details }
		if(unknown_wiki_ids.isNotEmpty()) {
			loadInterruptibly { VersionPreviewStore.wikiDetails(unknown_wiki_ids, allow_download = false) }.onSuccess { details ->
				wiki_details = wiki_details + details
			}
		}

		val unknown_image_files = nearby_version_ids
			.mapNotNull { version_id -> wiki_details[version_id]?.image_file }
			.filter { image_file -> image_file !in wiki_image_urls }
			.distinct()
		if(unknown_image_files.isNotEmpty()) {
			loadInterruptibly { VersionPreviewStore.wikiImageUrls(unknown_image_files, allow_download = false) }.onSuccess { image_urls ->
				wiki_image_urls = wiki_image_urls + image_urls
			}
		}

		nearby_version_ids
			.mapNotNull { version_id -> cardPreview(version_id, mojang_image_urls, wiki_details, wiki_image_urls)?.image_url }
			.distinct()
			.filter { image_url -> image_url !in thumbnails }
			.forEach { image_url ->
				loadInterruptibly { VersionPreviewStore.cachedImage(image_url)?.let { image_bytes -> decodeImage(image_bytes) } }.onSuccess { image ->
					if(image != null) {
						thumbnails = thumbnails + (image_url to image)
					}
				}
			}
	}

	LaunchedEffect(visible_version_ids) {
		if(visible_version_ids.isEmpty()) {
			return@LaunchedEffect
		}

		delay(scroll_settle_millis)

		coroutineScope {
			launch {
				val missing_ids = visible_version_ids.filter { version_id -> version_id !in mojang_image_urls }
				if(missing_ids.isNotEmpty()) {
					loadInterruptibly { VersionPreviewStore.patchNoteImageUrls(missing_ids) }.onSuccess { image_urls ->
						mojang_image_urls = mojang_image_urls + image_urls
					}
				}
			}

			launch {
				val missing_ids = visible_version_ids.filter { version_id -> version_id !in wiki_details }
				if(missing_ids.isNotEmpty()) {
					loadInterruptibly { VersionPreviewStore.wikiDetails(missing_ids) }.onSuccess { details ->
						wiki_details = wiki_details + details
					}
				}
			}
		}

		val needed_image_files = visible_version_ids
			.filter { version_id -> version_id in mojang_image_urls && mojang_image_urls[version_id] == null }
			.mapNotNull { version_id -> wiki_details[version_id]?.image_file }
			.filter { image_file -> image_file !in wiki_image_urls }
			.distinct()

		if(needed_image_files.isNotEmpty()) {
			loadInterruptibly { VersionPreviewStore.wikiImageUrls(needed_image_files) }.onSuccess { image_urls ->
				wiki_image_urls = wiki_image_urls + image_urls
			}
		}
	}

	Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
		Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
			QuantumTextField(label = null, value = search_text, onValueChange = onSearchChange, width = 260.dp, placeholder = "Search versions")

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
						val preview = cardPreview(version_choice.version_id, mojang_image_urls, wiki_details, wiki_image_urls)

						VersionCard(
							title = version_choice.title,
							preview = preview,
							thumbnail = preview?.image_url?.let { image_url -> thumbnails[image_url] },
							is_selected = version_choice.value == selected_version,
							is_visible = version_choice.value in visible_keys,
							onThumbnailLoaded = { image_url, image -> thumbnails = thumbnails + (image_url to image) },
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

private fun cardPreview(
	version_id: String,
	mojang_image_urls: Map<String, String?>,
	wiki_details: Map<String, WikiVersionDetails?>,
	wiki_image_urls: Map<String, String?>
): VersionCardPreview? {
	val is_mojang_checked = version_id in mojang_image_urls
	val is_wiki_checked = version_id in wiki_details

	if(!is_mojang_checked && !is_wiki_checked) {
		return null
	}

	val details = wiki_details[version_id]
	val wiki_image_file = details?.image_file
	val mojang_image_url = mojang_image_urls[version_id]
	val wiki_image_url = wiki_image_file?.let { image_file -> wiki_image_urls[image_file] }
	val is_wiki_image_checked = wiki_image_file == null || wiki_image_file in wiki_image_urls

	val is_image_missing = is_mojang_checked && mojang_image_url == null && is_wiki_checked && is_wiki_image_checked && wiki_image_url == null

	return VersionCardPreview(mojang_image_url ?: wiki_image_url, details?.name, is_image_missing)
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