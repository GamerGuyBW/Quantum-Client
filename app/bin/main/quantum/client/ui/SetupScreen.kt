package quantum.client.ui

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import quantum.shared.SetupTaskState
import quantum.shared.SetupTaskStatus
import quantum.shared.VersionSetup

@Composable
fun SetupScreen(onComplete: () -> Unit) {
	var statuses by remember { mutableStateOf(VersionSetup.statuses()) }
	val scroll_state = rememberScrollState()

	LaunchedEffect(Unit) {
		while(true) {
			statuses = VersionSetup.statuses()

			if(VersionSetup.isComplete()) {
				onComplete()
				break
			}

			delay(250)
		}
	}

	CompositionLocalProvider(LocalScrollbarStyle provides quantum_scrollbar_style) {
		Box(modifier = Modifier.fillMaxSize().background(QuantumColours.background)) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(scroll_state)
					.padding(horizontal = 48.dp, vertical = 40.dp),
				verticalArrangement = Arrangement.spacedBy(14.dp)
			) {
				BasicText(
					text = "Preparing Quantum Client…",
					style = QuantumTextStyles.heading.copy(fontSize = 24.sp)
				)
				BasicText(
					text = "Checking which game versions each mod loader supports. The first run takes the longest. Later runs only check new versions.",
					style = QuantumTextStyles.label
				)

				statuses.forEach { status -> SetupTaskRow(status) }

				if(statuses.any { status -> status.state == SetupTaskState.FAILED }) {
					Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
						BasicText(text = "Some checks could not be completed. Check your internet connection.", style = QuantumTextStyles.error)
						QuantumButton(text = "Retry", style = ButtonStyle.SECONDARY, onClick = { VersionSetup.retryFailed() })
					}
				}
			}

			VerticalScrollbar(
				adapter = rememberScrollbarAdapter(scroll_state),
				modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
			)
		}
	}
}

@Composable
private fun SetupTaskRow(status: SetupTaskStatus) {
	val status_text = when(status.state) {
		SetupTaskState.WAITING -> "Waiting…"
		SetupTaskState.RUNNING -> {
			if(status.total_count > 0) {
				"${"%,d".format(status.checked_count)} of ${"%,d".format(status.total_count)} checked"
			} else {
				"Loading…"
			}
		}
		SetupTaskState.DONE -> "Ready"
		SetupTaskState.FAILED -> "Failed"
	}

	val status_style = when(status.state) {
		SetupTaskState.DONE -> QuantumTextStyles.body.copy(color = QuantumColours.turquoise)
		SetupTaskState.FAILED -> QuantumTextStyles.error
		else -> QuantumTextStyles.body.copy(color = QuantumColours.text_secondary)
	}

	Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
		BasicText(
			text = status.name,
			style = QuantumTextStyles.body.copy(fontWeight = FontWeight.Medium),
			modifier = Modifier.width(200.dp)
		)
		BasicText(text = status_text, style = status_style)
	}
}