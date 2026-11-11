package quantum.client.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import quantum.shared.VersionPreview
import quantum.shared.VersionPreviewStore

@Composable
fun VersionCard(version_id: String, title: String, is_selected: Boolean, onClick: () -> Unit) {
	val shape = RoundedCornerShape(10.dp)
	val border_colour = if(is_selected) QuantumColours.blue else QuantumColours.border
	val border_width = if(is_selected) 2.dp else 1.dp

	val preview by produceState<VersionPreview?>(null, version_id) {
		value = loadQuietly { VersionPreviewStore.loadPreview(version_id) }
	}

	val image_url = preview?.image_url

	val thumbnail by produceState<ImageBitmap?>(null, image_url) {
		value = if(image_url == null) {
			null
		} else {
			loadQuietly { SkiaImage.makeFromEncoded(VersionPreviewStore.loadThumbnail(image_url)).toComposeImageBitmap() }
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

private suspend fun <T> loadQuietly(load: () -> T): T? {
	return try {
		withContext(Dispatchers.IO) { load() }
	} catch(error: CancellationException) {
		throw error
	} catch(error: Exception) {
		null
	}
}