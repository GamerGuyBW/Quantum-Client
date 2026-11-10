package quantum.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import quantum.shared.Edition
import quantum.shared.GameVersions
import quantum.shared.Instance

@Composable
fun InstanceCard(instance: Instance, is_selected: Boolean, onClick: () -> Unit) {
	val shape = RoundedCornerShape(10.dp)
	val border_colour = if(is_selected) QuantumColours.blue else QuantumColours.border
	val version_name = GameVersions.displayName(instance.game_version)
	val details = if(instance.edition == Edition.JAVA) "$version_name · ${instance.loader.display_name}" else version_name

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(shape)
			.background(QuantumColours.surface)
			.border(1.dp, border_colour, shape)
			.clickable(onClick = onClick)
			.pointerHoverIcon(PointerIcon.Hand)
			.padding(12.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		BasicText(
			text = instance.name,
			style = QuantumTextStyles.body.copy(fontWeight = FontWeight.Medium),
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
		BasicText(
			text = details,
			style = QuantumTextStyles.label,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
	}
}