package quantum.client.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object QuantumTextStyles {
	val title = TextStyle(color = QuantumColours.text_primary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, fontFamily = QuantumFonts.inter)
	val heading = TextStyle(color = QuantumColours.text_primary, fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = QuantumFonts.inter)
	val body = TextStyle(color = QuantumColours.text_primary, fontSize = 14.sp, fontFamily = QuantumFonts.inter)
	val label = TextStyle(color = QuantumColours.text_secondary, fontSize = 13.sp, fontFamily = QuantumFonts.inter)
	val small = TextStyle(color = QuantumColours.text_secondary, fontSize = 11.sp, fontFamily = QuantumFonts.inter)
	val error = TextStyle(color = QuantumColours.red, fontSize = 13.sp, fontFamily = QuantumFonts.inter)
}