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
			.background(QuantumColours.sidebar)
			.padding(horizontal = 10.dp, vertical = 16.dp),
		verticalArrangement = Arrangement.spacedBy(6.dp)
	) {
		Logo()

		Edition.entries.forEach { edition ->
			SidebarTab(
				edition = edition,
				is_selected = edition == selected_edition,
				onClick = { onEditionSelected(edition) }
			)
		}
	}
}

@Composable
fun Logo() {
	val logo_style = QuantumTextStyles.heading.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

	Row(modifier = Modifier.padding(start = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
		BasicText(text = "Quantum ", style = logo_style)
		BasicText(text = "Client", style = logo_style.copy(color = QuantumColours.red))
	}
}

@Composable
fun SidebarTab(edition: Edition, is_selected: Boolean, onClick: () -> Unit) {
	val background_colour = if(is_selected) QuantumColours.surface else Color.Transparent
	val text_colour = if(is_selected) QuantumColours.text_primary else QuantumColours.text_secondary
	val indicator_colour = if(is_selected) QuantumColours.turquoise else Color.Transparent
	val text_weight = if(is_selected) FontWeight.Medium else FontWeight.Normal

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(40.dp)
			.clip(RoundedCornerShape(8.dp))
			.background(background_colour)
			.clickable(onClick = onClick)
			.pointerHoverIcon(PointerIcon.Hand),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(indicator_colour))

		Row(
			modifier = Modifier.padding(start = 12.dp),
			horizontalArrangement = Arrangement.spacedBy(10.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			EditionIcon(edition = edition, is_selected = is_selected)
			BasicText(text = "${edition.display_name} Edition", style = QuantumTextStyles.body.copy(color = text_colour, fontWeight = text_weight))
		}
	}
}