package quantum.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import quantum.shared.Edition

@Composable
fun Sidebar(selected_edition: Edition, onEditionSelected: (Edition) -> Unit) {
	Column(
		modifier = Modifier
			.width(220.dp)
			.fillMaxHeight()
			.background(QuantumColors.sidebar)
			.padding(horizontal = 10.dp, vertical = 16.dp),
		verticalArrangement = Arrangement.spacedBy(6.dp)
	) {
		Logo()

		Edition.entries.forEach { edition ->
			SidebarTab(
				text = edition.display_name,
				is_selected = edition == selected_edition,
				onClick = { onEditionSelected(edition) }
			)
		}
	}
}

@Composable
fun Logo() {
	Row(modifier = Modifier.padding(start = 8.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
		BasicText(text = "Quantum ", style = TextStyle(color = QuantumColors.text_primary, fontSize = 16.sp, fontWeight = FontWeight.Medium))
		BasicText(text = "Client", style = TextStyle(color = QuantumColors.red, fontSize = 16.sp, fontWeight = FontWeight.Medium))
	}
}

@Composable
fun SidebarTab(text: String, is_selected: Boolean, onClick: () -> Unit) {
	val background_color = if(is_selected) QuantumColors.surface else Color.Transparent
	val text_color = if(is_selected) QuantumColors.text_primary else QuantumColors.text_secondary
	val indicator_color = if(is_selected) QuantumColors.turquoise else Color.Transparent

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(38.dp)
			.clip(RoundedCornerShape(8.dp))
			.background(background_color)
			.clickable(onClick = onClick)
			.pointerHoverIcon(PointerIcon.Hand),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(indicator_color))
		BasicText(text = text, modifier = Modifier.padding(start = 12.dp), style = TextStyle(color = text_color, fontSize = 14.sp))
	}
}