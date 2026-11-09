package quantum.client.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
fun GroupHeading(group: Group, instance_count: Int, onClick: () -> Unit) {
	val arrow = if(group.is_collapsed) "▸" else "▾"

	Row(
		modifier = Modifier
			.padding(top = 8.dp)
			.clip(RoundedCornerShape(6.dp))
			.clickable(onClick = onClick)
			.pointerHoverIcon(PointerIcon.Hand)
			.padding(horizontal = 6.dp, vertical = 4.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		BasicText(text = arrow, style = QuantumTextStyles.label)
		BasicText(text = group.name, style = QuantumTextStyles.body.copy(fontWeight = FontWeight.Medium))
		BasicText(text = instance_count.toString(), style = QuantumTextStyles.label)
	}
}