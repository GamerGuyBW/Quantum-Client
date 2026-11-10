package quantum.client.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import quantum.shared.Group

@Composable
fun GroupHeading(
	group: Group,
	instance_count: Int,
	is_managed: Boolean,
	onToggleCollapsed: () -> Unit,
	onManage: () -> Unit
) {
	val arrow = if(group.is_collapsed) "▸" else "▾"
	val manage_colour = if(is_managed) QuantumColours.blue else QuantumColours.text_secondary
	val shape = RoundedCornerShape(6.dp)

	Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
		Row(
			modifier = Modifier
				.clip(shape)
				.clickable(onClick = onToggleCollapsed)
				.pointerHoverIcon(PointerIcon.Hand)
				.padding(horizontal = 6.dp, vertical = 4.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			BasicText(text = arrow, style = QuantumTextStyles.label)
			BasicText(text = group.name, style = QuantumTextStyles.body.copy(fontWeight = FontWeight.Medium))
			BasicText(text = instance_count.toString(), style = QuantumTextStyles.label)
		}

		Spacer(modifier = Modifier.weight(1f))

		BasicText(
			text = "⋯",
			style = QuantumTextStyles.body.copy(color = manage_colour),
			modifier = Modifier
				.clip(shape)
				.clickable(onClick = onManage)
				.pointerHoverIcon(PointerIcon.Hand)
				.padding(horizontal = 10.dp, vertical = 4.dp)
		)
	}
}