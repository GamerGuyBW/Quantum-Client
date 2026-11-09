package quantum.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import quantum.shared.Edition
import quantum.shared.Instance

@Composable
fun InstanceCard(instance: Instance) {
	val shape = RoundedCornerShape(10.dp)
	val details = if(instance.edition == Edition.JAVA) "${instance.game_version} · ${instance.loader.display_name}" else instance.game_version

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clip(shape)
			.background(QuantumColors.surface)
			.border(1.dp, QuantumColors.border, shape)
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