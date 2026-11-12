package quantum.client.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import quantum.shared.Edition
import quantum.shared.GameVersions
import quantum.shared.Instance
import quantum.shared.VersionPreviewStore
import quantum.shared.VersionStore

private const val instance_fade_millis = 220

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InstanceCard(instance: Instance, is_selected: Boolean, onClick: () -> Unit) {
	val shape = RoundedCornerShape(10.dp)
	val border_colour = if(is_selected) QuantumColours.blue else QuantumColours.border
	val border_width = if(is_selected) 2.dp else 1.dp

	var thumbnail by remember(instance.edition, instance.game_version) { mutableStateOf<ImageBitmap?>(null) }
	var is_image_missing by remember(instance.edition, instance.game_version) { mutableStateOf(false) }
	val image_alpha by animateFloatAsState(if(thumbnail != null || is_image_missing) 1f else 0f, tween(instance_fade_millis))

	LaunchedEffect(instance.edition, instance.game_version) {
		if(instance.edition != Edition.JAVA) {
			is_image_missing = true
			return@LaunchedEffect
		}

		loadInterruptibly { loadInstanceThumbnail(instance.game_version) }.onSuccess { image ->
			if(image == null) {
				is_image_missing = true
			} else {
				thumbnail = image
			}
		}
	}

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(shape)
			.background(QuantumColours.surface)
			.border(border_width, border_colour, shape)
			.clickable(onClick = onClick)
			.pointerHoverIcon(PointerIcon.Hand)
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(16f / 9f)
				.background(QuantumColours.sidebar)
		) {
			val loaded_thumbnail = thumbnail

			if(loaded_thumbnail != null) {
				Image(
					bitmap = loaded_thumbnail,
					contentDescription = null,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize().alpha(image_alpha)
				)
			} else if(is_image_missing) {
				DefaultThumbnail(modifier = Modifier.fillMaxSize().alpha(image_alpha))
			}

			BasicText(
				text = GameVersions.displayName(instance.game_version),
				style = QuantumTextStyles.small.copy(color = QuantumColours.text_primary),
				maxLines = 1,
				modifier = Modifier
					.align(Alignment.BottomStart)
					.padding(8.dp)
					.clip(RoundedCornerShape(6.dp))
					.background(QuantumColours.badge_background)
					.padding(horizontal = 7.dp, vertical = 2.dp)
			)
		}

		Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
			BasicText(
				text = instance.name,
				style = QuantumTextStyles.body.copy(fontWeight = FontWeight.Medium),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)

			if(instance.edition == Edition.JAVA) {
				FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
					QuantumChip(text = loaderChipText(instance))

					val generation = instance.generation
					if(generation != null) {
						QuantumChip(text = "Gen $generation")
					}

					instance.addons.forEach { instance_addon ->
						QuantumChip(text = "+ ${instance_addon.addon.display_name} ${instance_addon.version_name}", is_accent = true)
					}
				}
			}
		}
	}
}

private fun loaderChipText(instance: Instance): String {
	val loader = instance.loader
	val loader_name = if(loader.display_name == loader.family.display_name) loader.display_name else "${loader.family.display_name} · ${loader.display_name}"
	val loader_version = instance.loader_version_name ?: instance.loader_version

	return if(loader_version != null) "$loader_name $loader_version" else loader_name
}

private fun loadInstanceThumbnail(game_version: String): ImageBitmap? {
	val version_id = GameVersions.resolve(game_version, VersionStore.fetchManifest())
	val image_url = VersionPreviewStore.patchNoteImageUrls(listOf(version_id))[version_id]
		?: VersionPreviewStore.wikiDetails(listOf(version_id))[version_id]?.image_file?.let { image_file -> VersionPreviewStore.wikiImageUrls(listOf(image_file))[image_file] }
		?: return null

	return decodeImage(VersionPreviewStore.loadImage(image_url))
}