package quantum.client.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class SupportSection(
	val heading: String?,
	val lines: List<String>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SupportTooltip(
	title: String,
	sections: List<SupportSection>?,
	is_failed: Boolean,
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	val shape = RoundedCornerShape(8.dp)

	TooltipArea(
		tooltip = {
			Column(
				modifier = Modifier
					.widthIn(max = 460.dp)
					.clip(shape)
					.background(QuantumColours.surface)
					.border(1.dp, QuantumColours.border, shape)
					.padding(12.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				BasicText(text = "$title runs on", style = QuantumTextStyles.body.copy(fontWeight = FontWeight.Medium))

				if(sections == null) {
					if(is_failed) {
						BasicText(text = "Could not load the version list.", style = QuantumTextStyles.error)
					} else {
						BasicText(text = "Loading the version list…", style = QuantumTextStyles.label)
					}
				} else if(sections.all { section -> section.lines.isEmpty() }) {
					BasicText(text = "No versions.", style = QuantumTextStyles.label)
				} else {
					sections
						.filter { section -> section.lines.isNotEmpty() }
						.forEach { section ->
							Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
								if(section.heading != null) {
									BasicText(text = section.heading, style = QuantumTextStyles.body)
								}

								section.lines.forEach { line ->
									BasicText(text = line, style = QuantumTextStyles.label)
								}
							}
						}
				}
			}
		},
		modifier = modifier,
		delayMillis = 400,
		content = content
	)
}