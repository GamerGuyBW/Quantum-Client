package quantum.client.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import quantum.shared.VersionPreviewStore

private const val thumbnail_max_width = 384
private const val fade_millis = 220

data class VersionCardPreview(
	val image_url: String?,
	val name: String?,
	val is_image_missing: Boolean
)

@Composable
fun VersionCard(
	title: String,
	preview: VersionCardPreview?,
	thumbnail: ImageBitmap?,
	is_selected: Boolean,
	is_visible: Boolean,
	onThumbnailLoaded: (String, ImageBitmap) -> Unit,
	onClick: () -> Unit
) {
	val shape = RoundedCornerShape(10.dp)
	val border_colour = if(is_selected) QuantumColours.blue else QuantumColours.border
	val border_width = if(is_selected) 2.dp else 1.dp
	val image_url = preview?.image_url
	val latest_on_thumbnail_loaded by rememberUpdatedState(onThumbnailLoaded)
	val has_thumbnail = thumbnail != null
	val is_image_missing = preview?.is_image_missing == true
	val name = preview?.name

	val image_alpha by animateFloatAsState(if(has_thumbnail || is_image_missing) 1f else 0f, tween(fade_millis))
	val name_alpha by animateFloatAsState(if(name != null) 1f else 0f, tween(fade_millis))

	LaunchedEffect(image_url, is_visible, has_thumbnail) {
		if(is_visible && image_url != null && !has_thumbnail) {
			loadInterruptibly { decodeImage(VersionPreviewStore.loadImage(image_url)) }.onSuccess { image ->
				latest_on_thumbnail_loaded(image_url, image)
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
			if(thumbnail != null) {
				Image(
					bitmap = thumbnail,
					contentDescription = null,
					contentScale = ContentScale.Crop,
					modifier = Modifier.fillMaxSize().alpha(image_alpha)
				)
			} else if(is_image_missing) {
				DefaultThumbnail(modifier = Modifier.fillMaxSize().alpha(image_alpha))
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
				text = name ?: " ",
				style = QuantumTextStyles.label,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.alpha(name_alpha)
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

fun decodeImage(image_bytes: ByteArray): ImageBitmap {
	val source_image = SkiaImage.makeFromEncoded(image_bytes)

	if(source_image.width <= thumbnail_max_width) {
		return source_image.toComposeImageBitmap()
	}

	val scaled_width = thumbnail_max_width
	val scaled_height = maxOf(1, source_image.height * thumbnail_max_width / source_image.width)
	val surface = Surface.makeRasterN32Premul(scaled_width, scaled_height)

	try {
		surface.canvas.drawImageRect(
			source_image,
			Rect.makeWH(source_image.width.toFloat(), source_image.height.toFloat()),
			Rect.makeWH(scaled_width.toFloat(), scaled_height.toFloat()),
			SamplingMode.LINEAR,
			null,
			true
		)

		return surface.makeImageSnapshot().toComposeImageBitmap()
	} finally {
		surface.close()
		source_image.close()
	}
}