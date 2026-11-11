package quantum.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun QuantumButton(text: String, onClick: () -> Unit, style: ButtonStyle = ButtonStyle.PRIMARY) {
	val background_colour = when(style) {
		ButtonStyle.PRIMARY -> QuantumColours.turquoise
		ButtonStyle.SECONDARY -> Color.Transparent
		ButtonStyle.DANGER -> Color.Transparent
	}
	val border_colour = when(style) {
		ButtonStyle.PRIMARY -> QuantumColours.turquoise
		ButtonStyle.SECONDARY -> QuantumColours.border
		ButtonStyle.DANGER -> QuantumColours.red
	}
	val text_colour = when(style) {
		ButtonStyle.PRIMARY -> QuantumColours.on_turquoise
		ButtonStyle.SECONDARY -> QuantumColours.text_primary
		ButtonStyle.DANGER -> QuantumColours.red
	}
	val shape = RoundedCornerShape(8.dp)

	Box(
		modifier = Modifier
			.clip(shape)
			.background(background_colour)
			.border(1.dp, border_colour, shape)
			.clickable(onClick = onClick)
			.pointerHoverIcon(PointerIcon.Hand)
			.padding(horizontal = 20.dp, vertical = 10.dp),
		contentAlignment = Alignment.Center
	) {
		BasicText(text = text, style = QuantumTextStyles.body.copy(color = text_colour, fontWeight = FontWeight.Medium))
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
			cursorBrush = SolidColor(QuantumColours.turquoise),
			modifier = Modifier
				.width(320.dp)
				.clip(shape)
				.background(QuantumColours.surface)
				.border(1.dp, QuantumColours.border, shape)
				.padding(horizontal = 12.dp, vertical = 10.dp)
		)
	}
}

@Composable
fun QuantumCheckbox(text: String, is_checked: Boolean, is_enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
	val shape = RoundedCornerShape(4.dp)
	val box_colour = if(is_checked) QuantumColours.turquoise else QuantumColours.surface
	val border_colour = if(is_checked) QuantumColours.turquoise else QuantumColours.border
	val row_modifier = if(is_enabled) {
		Modifier
			.clickable { onCheckedChange(!is_checked) }
			.pointerHoverIcon(PointerIcon.Hand)
	} else {
		Modifier.alpha(0.4f)
	}

	Row(
		modifier = row_modifier.padding(vertical = 4.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.size(18.dp)
				.clip(shape)
				.background(box_colour)
				.border(1.dp, border_colour, shape),
			contentAlignment = Alignment.Center
		) {
			if(is_checked) {
				BasicText(text = "✓", style = QuantumTextStyles.label.copy(color = QuantumColours.on_turquoise, fontWeight = FontWeight.Bold))
			}
		}

		BasicText(text = text, style = QuantumTextStyles.body)
	}
}