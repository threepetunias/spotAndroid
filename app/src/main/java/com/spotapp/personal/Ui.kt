package com.spotapp.personal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun colors(): SpotColors = LocalColors.current

@Composable
fun CircleBtn(
    icon: String, label: String, onClick: () -> Unit,
    size: Dp = 44.dp, bg: Color = colors().chip, iconSize: Dp = 22.dp, enabled: Boolean = true,
) {
    val c = colors()
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { SpotIcon(icon, if (enabled) c.ink else c.ink.copy(alpha = 0.4f), iconSize, hole = bg) }
}

@Composable
fun TopBar(
    title: String, back: Boolean = true, help: String? = null, cog: Boolean = false, insights: Boolean = false,
    right: @Composable () -> Unit = {},
) {
    val app = LocalApp.current
    val c = colors()
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (back) CircleBtn("back", "Back", { app.back() })
        Text(
            title, Modifier.weight(1f).padding(start = if (back) 4.dp else 8.dp).semantics { heading() },
            color = c.ink, fontFamily = Serif, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        right()
        if (insights) CircleBtn("chart", "Insights", { app.go(Route.Insights) })
        if (help != null) CircleBtn("help", "Help", { app.go(Route.Help(help)) })
        if (cog) CircleBtn("cog", "Settings", { app.goSettings(Route.Settings) })
    }
}

// Screen frame: top bar, scrolling content, optional pinned bottom bar and floating button.
@Composable
fun ScreenFrame(
    title: String, back: Boolean = true, help: String? = null, cog: Boolean = false, insights: Boolean = false,
    scroll: Boolean = true, scrollEnabled: Boolean = true, pad: Dp = 20.dp,
    right: @Composable () -> Unit = {},
    bottom: (@Composable () -> Unit)? = null, floating: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = colors()
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            TopBar(title, back, help, cog, insights, right)
            val m = if (scroll) Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState(), enabled = scrollEnabled) else Modifier.weight(1f).fillMaxWidth()
            Column(m.padding(horizontal = pad)) {
                content()
                if (bottom == null && floating != null) Spacer(Modifier.height(96.dp))
            }
            if (bottom != null) bottom()
            if (bottom == null) Spacer(Modifier.navigationBarsPadding())
        }
        if (floating != null) floating()
    }
}

// Shows nothing while the first load is under way (never a false "empty" message), and a plain message if it fails.
@Composable
fun <T> Loaded(state: Load<T>, content: @Composable (T) -> Unit) {
    val c = colors()
    val app = LocalApp.current
    when (state) {
        is Load.Ready -> content(state.value)
        is Load.Busy -> {}
        is Load.Failed -> Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("This could not be loaded. Your data is safe on this phone.", color = c.ink, fontSize = 17.sp, lineHeight = 24.sp)
            Row { BigBtn("Go back", { app.back() }, Modifier.weight(1f)) }
        }
    }
}

// Round floating "+" button at the bottom right, used for every "add" action.
@Composable
fun FloatingAdd(label: String, onClick: () -> Unit, raised: Boolean = false) {
    val c = colors()
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(end = 20.dp, bottom = if (raised) 88.dp else 12.dp), contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier.size(62.dp).clip(CircleShape).background(c.accent)
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { SpotIcon("plus", c.onAccent, 30.dp) }
    }
}

@Composable
fun Heading(text: String, size: Int = 22) {
    Text(
        text, Modifier.padding(top = 20.dp, bottom = 8.dp).semantics { heading() },
        color = colors().ink, fontFamily = Serif, fontSize = size.sp, fontWeight = FontWeight.Bold,
    )
}

@Composable
fun Caption(text: String) {
    Text(text.uppercase(), color = colors().muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
}

@Composable
fun Para(text: String, muted: Boolean = false, size: Int = 17, modifier: Modifier = Modifier) {
    Text(text, modifier, color = if (muted) colors().muted else colors().ink, fontSize = size.sp, lineHeight = (size + 7).sp)
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = colors()
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) c.accent else c.chip)
            .then(if (selected) Modifier else Modifier.border(BorderStroke(1.dp, c.line), RoundedCornerShape(22.dp)))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        if (selected) SpotIcon("check", c.onAccent, 16.dp)
        Text(label, color = if (selected) c.onAccent else c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

// Chips sit at their natural width and wrap onto the next line, so a long label is never cut off.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRows(content: @Composable () -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
fun BigBtn(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, secondary: Boolean = false, enabled: Boolean = true) {
    val c = colors()
    Box(
        modifier
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(if (secondary) c.chip else c.accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = (if (secondary) c.ink else c.onAccent).copy(alpha = if (enabled) 1f else 0.4f),
            fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        )
    }
}

// Cancel and Save sit in a bar pinned to the bottom of every sheet or form.
@Composable
fun PinnedBar(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(colors().bg).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
fun Tile(icon: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = colors()
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.chip)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(c.card), contentAlignment = Alignment.Center) { SpotIcon(icon, c.ink, 20.dp, hole = c.card) }
        Text(label, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
fun TileRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { content() }
}

@Composable
fun Field(
    label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    max: Int = 100, placeholder: String? = null, minLines: Int = 1, singleLine: Boolean = true, hint: String? = null,
    keyboard: KeyboardType = KeyboardType.Text, focus: FocusRequester? = null,
) {
    val c = colors()
    Column(modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(label, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= max) onChange(it) },
            modifier = Modifier.fillMaxWidth().then(if (focus != null) Modifier.focusRequester(focus) else Modifier).semantics { contentDescription = label },
            placeholder = if (placeholder != null) ({ Text(placeholder, color = c.muted) }) else null,
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(14.dp),
            textStyle = TextStyle(color = c.ink, fontSize = 18.sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.nowEdge, unfocusedBorderColor = c.edge,
                focusedContainerColor = c.card, unfocusedContainerColor = c.card, cursorColor = c.ink,
            ),
        )
        if (hint != null) Text(hint, color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors().line))
}

@Composable
fun ListRow(title: String, sub: String? = null, onClick: (() -> Unit)? = null, right: @Composable () -> Unit = {}) {
    val c = colors()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (sub != null) Text(sub, color = c.muted, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
        }
        right()
    }
}

@Composable
fun SheetRow(icon: String, label: String, onClick: () -> Unit) {
    val c = colors()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SpotIcon(icon, c.ink, 22.dp)
        Text(label, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

// A colored circle with a white drawn picture. Used for Settings topics and tiles.
@Composable
fun CircleIcon(icon: String, color: Color, size: Dp = 46.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        SpotIcon(icon, Color.White, size * 0.52f, hole = color)
    }
}

// Floating button on every Settings page below the main menu. `raised` lifts it above a pinned Cancel/Save bar.
@Composable
fun SettingsHome(raised: Boolean = false) {
    val app = LocalApp.current
    val c = colors()
    Box(
        Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = if (raised) 84.dp else 8.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(c.accent)
                .clickable(role = Role.Button, onClickLabel = "Settings home") { app.toSettingsHome() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SpotIcon("home", c.onAccent, 20.dp)
            Text("Settings home", color = c.onAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// For pages used both inside and outside Settings: shows the button only when the person came from Settings.
@Composable
fun SettingsHomeHere(raised: Boolean = false) {
    val app = LocalApp.current
    if (app.inSettings) SettingsHome(raised)
}

// The one place messages and confirmations show.
@Composable
fun ConfirmHost() {
    val app = LocalApp.current
    val c = colors()
    val cf = app.confirm ?: return
    AlertDialog(
        onDismissRequest = { app.confirm = null },
        containerColor = c.bg,
        title = { Text(cf.title, color = c.ink, fontFamily = Serif, fontWeight = FontWeight.Bold) },
        text = { Text(cf.message, color = c.ink, fontSize = 16.sp) },
        confirmButton = {
            TextButton(onClick = { app.confirm = null; cf.onOk() }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(cf.okLabel, color = if (cf.destructive) c.ink else c.accentText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        dismissButton = if (cf.cancelLabel != null) ({
            TextButton(onClick = { app.confirm = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(cf.cancelLabel, color = c.ink, fontSize = 16.sp)
            }
        }) else null,
    )
}

@Composable
fun LinkText(text: String, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 44.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, color = colors().accentText, fontSize = 16.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
    }
}

// Bottom sheet. Closing it only closes the sheet.
@Composable
fun SpotSheet(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = colors()
    ModalBottomSheet(onDismissRequest = onClose, containerColor = c.bg) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 12.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, Modifier.weight(1f).semantics { heading() }, color = c.ink, fontFamily = Serif, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                CircleBtn("close", "Close", onClose)
            }
            content()
        }
    }
}

// A small box that asks for one name. Used for "New ..." choices.
@Composable
fun PromptDialog(
    title: String, onCancel: () -> Unit, onSubmit: (String) -> Unit,
    confirmLabel: String = "Add", initial: String = "", maxLength: Int = 40, placeholder: String? = null,
    url: Boolean = false, allowEmpty: Boolean = false,
) {
    val c = colors()
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = c.bg,
        title = { Text(title, color = c.ink, fontFamily = Serif, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { if (it.length <= maxLength) text = it }, singleLine = true,
                placeholder = if (placeholder != null) ({ Text(placeholder, color = c.muted) }) else null,
                modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = title },
                shape = RoundedCornerShape(14.dp), textStyle = TextStyle(color = c.ink, fontSize = 18.sp),
                keyboardOptions = KeyboardOptions(keyboardType = if (url) KeyboardType.Uri else KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.nowEdge, unfocusedBorderColor = c.edge,
                    focusedContainerColor = c.card, unfocusedContainerColor = c.card, cursorColor = c.ink,
                ),
            )
            LaunchedEffect(Unit) { focus.requestFocus() }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(text) }, enabled = allowEmpty || text.trim().isNotEmpty(), modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(confirmLabel, color = c.accentText, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel", color = c.ink, fontSize = 16.sp) }
        },
    )
}

// A dashed "+ New ..." chip that sits at the end of a row of choices.
@Composable
fun AddChip(label: String, onClick: () -> Unit) {
    val c = colors()
    Row(
        Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp)).border(2.dp, c.edge, RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SpotIcon("plus", c.ink, 16.dp)
        Text(label, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

class Option(val id: Long, val label: String, val icon: String = "")

// Pick one. Tapping the chosen one again clears it when "clearable" is set.
@Composable
fun ChoiceChips(options: List<Option>, value: Long?, onChange: (Long?) -> Unit, clearable: Boolean = false, addLabel: String? = null, onAdd: (() -> Unit)? = null) {
    ChipRows {
        for (o in options) Chip(o.label, o.id == value, { onChange(if (o.id == value && clearable) null else o.id) })
        if (onAdd != null) AddChip(addLabel ?: "New", onAdd)
    }
}

// Pick any number.
@Composable
fun MultiChips(options: List<Option>, value: List<Long>, onToggle: (Long) -> Unit, addLabel: String? = null, onAdd: (() -> Unit)? = null) {
    ChipRows {
        for (o in options) Chip(o.label, o.id in value, { onToggle(o.id) })
        if (onAdd != null) AddChip(addLabel ?: "New", onAdd)
    }
}

// Picture tiles, such as workout types.
@Composable
fun TypeTiles(options: List<Option>, value: Long?, onChange: (Long?) -> Unit, addLabel: String? = null, onAdd: (() -> Unit)? = null) {
    val c = colors()
    @Composable
    fun tile(label: String, on: Boolean, onClick: () -> Unit, body: @Composable () -> Unit) {
        Column(
            Modifier.width(92.dp).heightIn(min = 92.dp).clip(RoundedCornerShape(16.dp)).background(c.card)
                .border(3.dp, if (on) c.accent else c.card, RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { this.selected = on; contentDescription = label }
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            body()
            Text(label, color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    ChipRows {
        for (o in options) {
            val on = o.id == value
            tile(o.label, on, { onChange(if (on) null else o.id) }) {
                if (o.icon.isNotEmpty()) WorkoutPicture(o.icon, 44.dp)
                else Box(Modifier.size(44.dp).clip(CircleShape).background(c.chip), contentAlignment = Alignment.Center) { SpotIcon("check", c.ink, 20.dp) }
            }
        }
        if (onAdd != null) tile(addLabel ?: "New", false, onAdd) {
            Box(Modifier.size(44.dp).clip(CircleShape).border(2.dp, c.edge, CircleShape), contentAlignment = Alignment.Center) { SpotIcon("plus", c.ink, 20.dp) }
        }
    }
}

// A row of two to five choices that share the width, such as Last | Best | Other.
@Composable
fun Segmented(options: List<Pair<String, String>>, value: String, onChange: (String) -> Unit) {
    val c = colors()
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.chip).padding(4.dp)) {
        for ((key, label) in options) {
            val on = key == value
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(20.dp)).background(if (on) c.accent else Color.Transparent)
                    .clickable(role = Role.Button, onClick = { onChange(key) }).semantics { this.selected = on },
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (on) c.onAccent else c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// Shows a big number. Tap it and a small box opens to type the number. A bad number is rejected and the box stays open.
@Composable
fun NumberField(value: Double, label: String, size: Int, max: Double, integer: Boolean = false, unit: String? = null, onCommit: (Double) -> Unit) {
    val c = colors()
    var open by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    fun submit() {
        val n = parseTyped(text)
        if (n == null || n > max || (integer && n != Math.floor(n))) { bad = true; return }
        onCommit(n)
        open = false
    }

    Box(
        Modifier.widthIn(min = (size * 1.9f).dp).heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).background(c.card)
            .border(1.5.dp, c.edge, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = "Type $label") { text = if (value == 0.0) "" else num(value); bad = false; open = true }
            .semantics { contentDescription = "$label ${num(value)}. Tap to type a number." }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(num(value), color = c.ink, fontFamily = Serif, fontSize = size.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = c.bg,
            title = { Text(if (unit != null) "$label ($unit)" else label, color = c.ink, fontFamily = Serif, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = text, onValueChange = { if (it.length <= 7) { text = it; bad = false } }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "$label, type a number" },
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(color = c.ink, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.nowEdge, unfocusedBorderColor = c.edge,
                            focusedContainerColor = c.card, unfocusedContainerColor = c.card, cursorColor = c.ink,
                        ),
                    )
                    if (bad) Text(if (integer) "Type a whole number from 0 to ${max.toLong()}." else "Type a number from 0 to ${max.toLong()}.", color = c.ink, fontSize = 15.sp)
                }
                LaunchedEffect(Unit) { focus.requestFocus() }
            },
            confirmButton = { TextButton(onClick = { submit() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Set", color = c.accentText, fontWeight = FontWeight.Bold, fontSize = 16.sp) } },
            dismissButton = { TextButton(onClick = { open = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel", color = c.ink, fontSize = 16.sp) } },
        )
    }
}

// Bottom menu. The four tabs replace each other (no sliding), so it feels like one app with four rooms.
@Composable
fun TabBar(current: Route?) {
    val app = LocalApp.current
    val c = colors()
    data class T(val route: Route, val label: String, val icon: String)
    val tabs = listOf(T(Route.Home, "Home", "home"), T(Route.Prs, "PRs", "trophy"), T(Route.Timer, "Timer", "timer"), T(Route.Log, "Log", "list"))
    Row(Modifier.fillMaxWidth().background(c.card).navigationBarsPadding().padding(top = 6.dp, bottom = 4.dp)) {
        for (t in tabs) {
            val on = t.route == current
            Column(
                Modifier.weight(1f).heightIn(min = 56.dp)
                    .clickable(role = Role.Tab, onClickLabel = t.label) { if (!on) app.tab(t.route) }
                    .semantics { this.selected = on; contentDescription = t.label },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
            ) {
                Box(Modifier.width(56.dp).height(30.dp).clip(RoundedCornerShape(15.dp)).background(if (on) c.accent else Color.Transparent), contentAlignment = Alignment.Center) {
                    SpotIcon(t.icon, if (on) c.onAccent else c.muted, 24.dp)
                }
                Text(t.label, color = if (on) c.ink else c.muted, fontSize = 13.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold)
            }
        }
    }
}
