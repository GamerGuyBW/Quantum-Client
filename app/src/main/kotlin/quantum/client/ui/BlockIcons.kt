package quantum.client.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import quantum.shared.Edition

@Composable
fun EditionIcon(edition: Edition, is_selected: Boolean, size: Dp = 20.dp) {
	val icon_modifier = Modifier.size(size).alpha(if(is_selected) 1f else 0.55f)

	when(edition) {
		Edition.JAVA -> BlockIcon(
			top_colour = QuantumColours.grass_top,
			left_colour = QuantumColours.dirt_left,
			right_colour = QuantumColours.dirt_right,
			band_colour = QuantumColours.grass_band,
			modifier = icon_modifier
		)
		Edition.BEDROCK -> BlockIcon(
			top_colour = QuantumColours.bedrock_top,
			left_colour = QuantumColours.bedrock_left,
			right_colour = QuantumColours.bedrock_right,
			band_colour = null,
			modifier = icon_modifier
		)
	}
}

@Composable
private fun BlockIcon(top_colour: Color, left_colour: Color, right_colour: Color, band_colour: Color?, modifier: Modifier = Modifier) {
	Canvas(modifier = modifier) {
		val cube_radius = size.minDimension / 2f
		val side_offset = cube_radius * 0.866f
		val half_radius = cube_radius / 2f
		val band_height = cube_radius * 0.28f

		val top = Offset(center.x, center.y - cube_radius)
		val upper_right = Offset(center.x + side_offset, center.y - half_radius)
		val lower_right = Offset(center.x + side_offset, center.y + half_radius)
		val bottom = Offset(center.x, center.y + cube_radius)
		val lower_left = Offset(center.x - side_offset, center.y + half_radius)
		val upper_left = Offset(center.x - side_offset, center.y - half_radius)

		drawPath(facePath(top, upper_right, center, upper_left), color = top_colour)
		drawPath(facePath(upper_left, center, bottom, lower_left), color = left_colour)
		drawPath(facePath(center, upper_right, lower_right, bottom), color = right_colour)

		if(band_colour != null) {
			val band_offset = Offset(0f, band_height)
			drawPath(facePath(upper_left, center, center + band_offset, upper_left + band_offset), color = band_colour)
			drawPath(facePath(center, upper_right, upper_right + band_offset, center + band_offset), color = band_colour)
		}
	}
}

private fun facePath(first: Offset, second: Offset, third: Offset, fourth: Offset): Path {
	return Path().apply {
		moveTo(first.x, first.y)
		lineTo(second.x, second.y)
		lineTo(third.x, third.y)
		lineTo(fourth.x, fourth.y)
		close()
	}
}