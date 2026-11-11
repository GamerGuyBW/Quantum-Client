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
import org.jetbrains.skia.Image as SkiaImage
import quantum.shared.VersionPreviewStore

data class VersionCardPreview(
	val image_url: String?,
	val name: String?,
	val is_image_missing: Boolean
)

@Composable
fun VersionCard(version_id: String, title: String, preview: VersionCardPreview?, is_selected: Boolean, is_visible: Boolean, onClick: () -> Unit) {
	val shape = RoundedCornerShape(10.dp)
	val border_colour = if(is_selected) QuantumColours.blue else QuantumColours.border
	val border_width = if(is_selected) 2.dp else 1.dp
	val image_url = preview?.image_url

	var thumbnail by remember(version_id, image_url) { mutableStateOf<ImageBitmap?>(null) }

	LaunchedEffect(image_url, is_visible) {
		if(is_visible && image_url != null && thumbnail == null) {
			loadInterruptibly { SkiaImage.makeFromEncoded(VersionPreviewStore.loadImage(image_url)).toComposeImageBitmap() }.onSuccess { image ->
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
					modifier = Modifier.fillMaxSize()
				)
			} else if(preview?.is_image_missing == true) {
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
				text = preview?.name ?: " ",
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