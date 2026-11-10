package quantum.client.ui

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val quantum_scrollbar_style = ScrollbarStyle(
	minimalHeight = 24.dp,
	thickness = 8.dp,
	shape = RoundedCornerShape(4.dp),
	hoverDurationMillis = 200,
	unhoverColor = QuantumColours.border,
	hoverColor = QuantumColours.text_secondary
)