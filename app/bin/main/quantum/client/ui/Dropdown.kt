package quantum.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch

data class DropdownOption<T>(val value: T, val label: String)

@Composable
fun <T> QuantumDropdown(
	options: List<DropdownOption<T>>,
	selected_value: T,
	onValueSelected: (T) -> Unit,
	label: String? = null,
	width: Dp = 320.dp
) {
	var is_open by remember { mutableStateOf(false) }
	val selected_label = options.firstOrNull { option -> option.value == selected_value }?.label ?: ""
	val border_colour = if(is_open) QuantumColours.blue else QuantumColours.border
	val shape = RoundedCornerShape(8.dp)

	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		if(label != null) {
			BasicText(text = label, style = QuantumTextStyles.label)
		}

		Box {
			Row(
				modifier = Modifier
					.width(width)
					.clip(shape)
					.background(QuantumColours.surface)
					.border(1.dp, border_colour, shape)
					.clickable { is_open = !is_open }
					.pointerHoverIcon(PointerIcon.Hand)
					.padding(horizontal = 12.dp, vertical = 10.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				BasicText(
					text = selected_label,
					style = QuantumTextStyles.body,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
				BasicText(text = "▾", style = QuantumTextStyles.label)
			}

			if(is_open) {
				Popup(
					popupPositionProvider = remember { DropdownPositionProvider() },
					onDismissRequest = { is_open = false },
					properties = PopupProperties(focusable = true)
				) {
					DropdownList(
						options = options,
						selected_value = selected_value,
						width = width,
						onOptionChosen = { option ->
							onValueSelected(option.value)
							is_open = false
						},
						onClose = { is_open = false }
					)
				}
			}
		}
	}
}

@Composable
private fun <T> DropdownList(
	options: List<DropdownOption<T>>,
	selected_value: T,
	width: Dp,
	onOptionChosen: (DropdownOption<T>) -> Unit,
	onClose: () -> Unit
) {
	val list_state = rememberLazyListState()
	val focus_requester = remember { FocusRequester() }
	val coroutine_scope = rememberCoroutineScope()
	var highlighted_index by remember { mutableStateOf(options.indexOfFirst { option -> option.value == selected_value }) }
	val shape = RoundedCornerShape(8.dp)

	fun highlight(index: Int) {
		highlighted_index = index
		coroutine_scope.launch { list_state.animateScrollToItem(index) }
	}

	LaunchedEffect(Unit) {
		focus_requester.requestFocus()
		if(highlighted_index >= 0) {
			list_state.scrollToItem(highlighted_index)
		}
	}

	Box(
		modifier = Modifier
			.padding(vertical = 4.dp)
			.width(width)
			.heightIn(max = 240.dp)
			.clip(shape)
			.background(QuantumColours.surface)
			.border(1.dp, QuantumColours.border, shape)
			.focusRequester(focus_requester)
			.focusable()
			.onPreviewKeyEvent { event ->
				if(event.type != KeyEventType.KeyDown) {
					false
				} else if(event.key == Key.Enter) {
					val highlighted_option = options.getOrNull(highlighted_index)
					if(highlighted_option != null) {
						onOptionChosen(highlighted_option)
					} else {
						onClose()
					}
					true
				} else if(event.key == Key.Escape) {
					onClose()
					true
				} else if(event.key == Key.DirectionDown) {
					if(options.isNotEmpty()) {
						highlight((highlighted_index + 1).coerceAtMost(options.lastIndex))
					}
					true
				} else if(event.key == Key.DirectionUp) {
					if(options.isNotEmpty()) {
						highlight((highlighted_index - 1).coerceAtLeast(0))
					}
					true
				} else {
					val match_index = findTypeAheadMatch(options, event.utf16CodePoint.toChar(), highlighted_index)
					if(match_index != null) {
						highlight(match_index)
						true
					} else {
						false
					}
				}
			}
	) {
		LazyColumn(state = list_state) {
			itemsIndexed(options) { index, option ->
				val background_colour = if(index == highlighted_index) QuantumColours.border else Color.Transparent
				val text_colour = if(option.value == selected_value) QuantumColours.blue else QuantumColours.text_primary

				BasicText(
					text = option.label,
					style = QuantumTextStyles.body.copy(color = text_colour),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier
						.fillMaxWidth()
						.background(background_colour)
						.clickable { onOptionChosen(option) }
						.pointerHoverIcon(PointerIcon.Hand)
						.padding(horizontal = 12.dp, vertical = 8.dp)
				)
			}
		}
	}
}

private fun <T> findTypeAheadMatch(options: List<DropdownOption<T>>, typed_character: Char, current_index: Int): Int? {
	if(!typed_character.isLetterOrDigit()) {
		return null
	}

	return (1..options.size)
		.map { step -> (current_index + step).mod(options.size) }
		.firstOrNull { index -> options[index].label.startsWith(typed_character, ignoreCase = true) }
}

private class DropdownPositionProvider : PopupPositionProvider {
	override fun calculatePosition(
		anchorBounds: IntRect,
		windowSize: IntSize,
		layoutDirection: LayoutDirection,
		popupContentSize: IntSize
	): IntOffset {
		val below_y = anchorBounds.bottom
		val above_y = anchorBounds.top - popupContentSize.height
		val fits_below = below_y + popupContentSize.height <= windowSize.height
		val position_y = if(fits_below || above_y < 0) below_y else above_y

		return IntOffset(anchorBounds.left, position_y)
	}
}