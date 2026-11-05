package com.spotapp.personal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Shows a big number. Tap it to type a value instead of using the + and - buttons.
@Composable
fun NumberField(value: Double, label: String, size: Int, max: Double, integer: Boolean, onCommit: (Double) -> Unit) {
    val c = colors()
    var editing by remember { mutableStateOf(false) }
    var tfv by remember { mutableStateOf(TextFieldValue("")) }
    var hadFocus by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val minW = (size * 1.6f).dp

    fun commit() {
        if (!editing) return
        editing = false
        val n = parseTyped(tfv.text)
        if (n != null && n <= max && (!integer || n == Math.floor(n))) onCommit(n)
    }

    if (editing) {
        BasicTextField(
            value = tfv,
            onValueChange = { if (it.text.length <= 7) tfv = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            textStyle = TextStyle(fontFamily = Serif, fontSize = size.sp, fontWeight = FontWeight.Bold, color = c.ink, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(c.ink),
            modifier = Modifier
                .widthIn(min = minW)
                .focusRequester(focus)
                .onFocusChanged {
                    if (hadFocus && !it.isFocused) commit()
                    hadFocus = it.isFocused
                }
                .semantics { contentDescription = "$label, type a number" },
        )
        LaunchedEffect(Unit) { focus.requestFocus() }
    } else {
        Text(
            num(value),
            Modifier
                .widthIn(min = minW)
                .clickable(role = Role.Button, onClickLabel = "Type $label") {
                    val s = num(value)
                    tfv = TextFieldValue(s, TextRange(0, s.length))
                    hadFocus = false
                    editing = true
                }
                .semantics { contentDescription = "$label ${num(value)}. Tap to type a number." },
            color = c.ink, fontFamily = Serif, fontSize = size.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        )
    }
}
