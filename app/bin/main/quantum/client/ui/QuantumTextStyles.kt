package quantum.client.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object QuantumTextStyles {
	val heading = TextStyle(color = QuantumColours.text_primary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
	val body = TextStyle(color = QuantumColours.text_primary, fontSize = 14.sp)
	val label = TextStyle(color = QuantumColours.text_secondary, fontSize = 13.sp)
	val error = TextStyle(color = QuantumColours.red, fontSize = 13.sp)
}