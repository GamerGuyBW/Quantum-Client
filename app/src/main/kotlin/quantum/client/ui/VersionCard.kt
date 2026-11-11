package quantum.client.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.jetbrains.skia.Image as SkiaImage
import quantum.shared.VersionPreviewStore
import quantum.shared.WikiVersionDetails

@Composable
fun VersionCard(version_id: String, title: String, is_selected: Boolean, is_visible: Boolean, onClick: () -> Unit) {
	val shape = RoundedCornerShape(10.dp)
	val border_colour = if(is_selected) QuantumColours.blue else QuantumColours.border
	val border_width = if(is_selected) 2.dp else 1.dp

	var mojang_image_url by remember(version_id) { mutableStateOf<String?>(null) }
	var is_mojang_checked by remember(version_id) { mutableStateOf(false) }
	var wiki_details by remember(version_id) { mutableStateOf<WikiVersionDetails?>(null) }
	var is_wiki_checked by remember(version_id) { mutableStateOf(false) }
	var thumbnail by remember(version_id) { mutableStateOf<ImageBitmap?>(null) }
	var is_thumbnail_missing by remember(version_id) { mutableStateOf(false) }

	LaunchedEffect(version_id, is_visible) {
		if(is_visible && !is_mojang_checked) {
			loadInterruptibly { VersionPreviewStore.patchNoteImageUrl(version_id) }.onSuccess { image_url ->
				mojang_image_url = image_url
				is_mojang_checked = true
			}
		}
	}

	LaunchedEffect(version_id, is_visible) {
		if(is_visible && !is_wiki_checked) {
			loadInterruptibly { VersionPreviewStore.wikiDetails(version_id) }.onSuccess { details ->
				wiki_details = details
				is_wiki_checked = true
			}
		}
	}

	LaunchedEffect(version_id, is_visible, is_mojang_checked, mojang_image_url, is_wiki_checked, wiki_details) {
		if(!is_visible || thumbnail != null || is_thumbnail_missing || !is_mojang_checked) {
			return@LaunchedEffect
		}

		val known_mojang_url = mojang_image_url
		val wiki_image_file = wiki_details?.image_file

		val image_url = if(known_mojang_url != null) {
			known_mojang_url
		} else if(!is_wiki_checked) {
			return@LaunchedEffect
		} else if(wiki_image_file == null) {
			is_thumbnail_missing = true
			return@LaunchedEffect
		} else {
			val wiki_image_result = loadInterruptibly { VersionPreviewStore.wikiImageUrl(wiki_image_file) }
			if(wiki_image_result.isFailure) {
				return@LaunchedEffect
			}
			wiki_image_result.getOrNull()
		}

		if(image_url == null) {
			is_thumbnail_missing = true
			return@LaunchedEffect
		}

		loadInterruptibly { SkiaImage.makeFromEncoded(VersionPreviewStore.loadImage(image_url)).toComposeImageBitmap() }.onSuccess { image ->
			thumbnail = image
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
					modifier = Modifier.fillMaxSize()
				)
			} else if(is_thumbnail_missing) {
				DefaultThumbnail(modifier = Modifier.fillMaxSize())
			}
		}

		Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
			BasicText(
				text = title,
				style = QuantumTextStyles.body.copy(fontWeight = FontWeight.Medium),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			BasicText(
				text = wiki_details?.name ?: " ",
				style = QuantumTextStyles.label,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}

@Composable
private fun DefaultThumbnail(modifier: Modifier = Modifier) {
	Canvas(modifier = modifier) {
		val cube_radius = size.minDimension * 0.27f
		val side_offset = cube_radius * 0.866f
		val half_radius = cube_radius / 2f

		val top = Offset(center.x, center.y - cube_radius)
		val upper_right = Offset(center.x + side_offset, center.y - half_radius)
		val lower_right = Offset(center.x + side_offset, center.y + half_radius)
		val bottom = Offset(center.x, center.y + cube_radius)
		val lower_left = Offset(center.x - side_offset, center.y + half_radius)
		val upper_left = Offset(center.x - side_offset, center.y - half_radius)

		val outline = Path().apply {
			moveTo(top.x, top.y)
			lineTo(upper_right.x, upper_right.y)
			lineTo(lower_right.x, lower_right.y)
			lineTo(bottom.x, bottom.y)
			lineTo(lower_left.x, lower_left.y)
			lineTo(upper_left.x, upper_left.y)
			close()
		}

		val inner_edges = Path().apply {
			moveTo(upper_left.x, upper_left.y)
			lineTo(center.x, center.y)
			lineTo(upper_right.x, upper_right.y)
			moveTo(center.x, center.y)
			lineTo(bottom.x, bottom.y)
		}

		val line_style = Stroke(width = 2.dp.toPx())

		drawPath(outline, color = QuantumColours.turquoise, style = line_style)
		drawPath(inner_edges, color = QuantumColours.turquoise, style = line_style)
	}
}

private suspend fun <T> loadInterruptibly(load: () -> T): Result<T> {
	return try {
		Result.success(runInterruptible(Dispatchers.IO) { load() })
	} catch(error: CancellationException) {
		throw error
	} catch(error: Exception) {
		Result.failure(error)
	}
}