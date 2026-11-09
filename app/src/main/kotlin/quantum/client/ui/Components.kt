package quantum.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun QuantumButton(text: String, onClick: () -> Unit, is_primary: Boolean = true) {
	val background_color = if(is_primary) QuantumColors.turquoise else Color.Transparent
	val border_color = if(is_primary) QuantumColors.turquoise else QuantumColors.border
	val text_color = if(is_primary) QuantumColors.on_turquoise else QuantumColors.text_primary
	val shape = RoundedCornerShape(8.dp)

	Box(
		modifier = Modifier
			.clip(shape)
			.background(background_color)
			.border(1.dp, border_color, shape)
			.clickable(onClick = onClick)
			.pointerHoverIcon(PointerIcon.Hand)
			.padding(horizontal = 20.dp, vertical = 10.dp),
		contentAlignment = Alignment.Center
	) {
		BasicText(text = text, style = QuantumTextStyles.body.copy(color = text_color, fontWeight = FontWeight.Medium))
	}
}

@Composable
fun QuantumTextField(label: String, value: String, onValueChange: (String) -> Unit) {
	val shape = RoundedCornerShape(8.dp)

	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		BasicText(text = label, style = QuantumTextStyles.label)

		BasicTextField(
			value = value,
			onValueChange = onValueChange,
			singleLine = true,
			textStyle = QuantumTextStyles.body,
			cursorBrush = SolidColor(QuantumColors.turquoise),
			modifier = Modifier
				.width(320.dp)
				.clip(shape)
				.background(QuantumColors.surface)
				.border(1.dp, QuantumColors.border, shape)
				.padding(horizontal = 12.dp, vertical = 10.dp)
		)
	}
}

@Composable
fun ChoiceChip(text: String, is_selected: Boolean, onClick: () -> Unit) {
	val border_color = if(is_selected) QuantumColors.blue else QuantumColors.border
	val text_color = if(is_selected) QuantumColors.text_primary else QuantumColors.text_secondary
	val shape = RoundedCornerShape(8.dp)

	Box(
		modifier = Modifier
			.clip(shape)
			.background(QuantumColors.surface)
			.border(1.dp, border_color, shape)
			.clickable(onClick = onClick)
			.pointerHoverIcon(PointerIcon.Hand)
			.padding(horizontal = 14.dp, vertical = 8.dp)
	) {
		BasicText(text = text, style = QuantumTextStyles.body.copy(color = text_color))
	}
}