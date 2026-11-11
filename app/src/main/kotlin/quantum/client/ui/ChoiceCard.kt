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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ChoiceCard(title: String, subtitle: String, is_selected: Boolean, is_enabled: Boolean, onClick: () -> Unit) {
	val shape = RoundedCornerShape(10.dp)
	val border_colour = if(is_selected) QuantumColours.blue else QuantumColours.border
	val border_width = if(is_selected) 2.dp else 1.dp

	val card_modifier = Modifier
		.fillMaxWidth()
		.clip(shape)
		.background(QuantumColours.surface)
		.border(border_width, border_colour, shape)

	val interactive_modifier = if(is_enabled) {
		card_modifier
			.clickable(onClick = onClick)
			.pointerHoverIcon(PointerIcon.Hand)
	} else {
		card_modifier.alpha(0.4f)
	}

	Column(modifier = interactive_modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
		BasicText(
			text = title,
			style = QuantumTextStyles.body.copy(fontWeight = FontWeight.Medium),
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
		BasicText(
			text = subtitle,
			style = QuantumTextStyles.label,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
	}
}