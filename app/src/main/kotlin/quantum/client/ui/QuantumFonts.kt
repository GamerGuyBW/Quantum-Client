package quantum.client.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

object QuantumFonts {
	val inter: FontFamily = FontFamily(
		interFont("Inter-Regular.ttf", FontWeight.Normal),
		interFont("Inter-Medium.ttf", FontWeight.Medium),
		interFont("Inter-SemiBold.ttf", FontWeight.SemiBold)
	)

	private fun interFont(file_name: String, weight: FontWeight): Font {
		val font_stream = QuantumFonts::class.java.getResourceAsStream("/fonts/$file_name") ?: throw IllegalStateException("The font `$file_name` is missing from the app resources.")
		val font_bytes = font_stream.use { stream -> stream.readBytes() }

		return Font(identity = file_name, data = font_bytes, weight = weight)
	}
}