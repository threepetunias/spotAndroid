package com.spotapp.personal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    ) { SpotIcon(icon, c.ink, iconSize, hole = bg) }
}

@Composable
fun TopBar(title: String, back: Boolean = true, help: Boolean = false, cog: Boolean = false, right: @Composable () -> Unit = {}) {
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
        if (help) CircleBtn("help", "Help", { app.go("help") })
        if (cog) CircleBtn("cog", "Settings", { app.go("settings") })
    }
}

// Screen frame: top bar, scrolling content, optional pinned bottom bar.
@Composable
fun ScreenFrame(
    title: String, back: Boolean = true, help: Boolean = false, cog: Boolean = false,
    scroll: Boolean = true, bottom: (@Composable () -> Unit)? = null, floating: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = colors()
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            TopBar(title, back, help, cog)
            val m = if (scroll) Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()) else Modifier.weight(1f).fillMaxWidth()
            Column(m.padding(horizontal = 20.dp)) {
                content()
                if (bottom == null && floating != null) Spacer(Modifier.height(96.dp))
            }
            if (bottom != null) bottom()
            if (bottom == null) Spacer(Modifier.navigationBarsPadding())
        }
        if (floating != null) floating()
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
fun Para(text: String, muted: Boolean = false, size: Int = 17) {
    Text(text, color = if (muted) colors().muted else colors().ink, fontSize = size.sp, lineHeight = (size + 7).sp)
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

// Chip rows never hold more than three, so nothing is cut off at the screen edge.
fun chipItem(label: String, selected: Boolean, onClick: () -> Unit): @Composable (Modifier) -> Unit = { m -> Chip(label, selected, onClick, m) }

@Composable
fun ChipRows(items: List<@Composable (Modifier) -> Unit>, perRow: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(perRow).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { it(Modifier.weight(1f)) }
                repeat(perRow - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
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
        Box(Modifier.size(36.dp).clip(CircleShape).background(c.card), contentAlignment = Alignment.Center) { SpotIcon(icon, c.ink, 20.dp) }
        Text(label, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
fun TileRow(items: List<@Composable (Modifier) -> Unit>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { it(Modifier.weight(1f)) }
    }
}

@Composable
fun Field(
    label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    max: Int = 100, placeholder: String? = null, minLines: Int = 1, singleLine: Boolean = true, hint: String? = null,
) {
    val c = colors()
    Column(modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(label, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= max) onChange(it) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            placeholder = if (placeholder != null) ({ Text(placeholder, color = c.muted) }) else null,
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(14.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = c.ink, fontSize = 18.sp),
            keyboardOptions = KeyboardOptions.Default,
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
fun MenuRow(title: String, sub: String? = null, onClick: (() -> Unit)? = null) {
    val c = colors()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (sub != null) Text(sub, color = c.muted, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
        }
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

// Floating button on every Settings page below the main menu.
@Composable
fun SettingsHome() {
    val app = LocalApp.current
    val c = colors()
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
        Row(
            Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(c.accent)
                .clickable(role = Role.Button, onClickLabel = "Settings home") { app.nav.popBackStack("settings", false) }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SpotIcon("home", c.onAccent, 20.dp)
            Text("Settings home", color = c.onAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
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
                Text(cf.okLabel, color = c.accentText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotSheet(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = colors()
    ModalBottomSheet(onDismissRequest = onClose, containerColor = c.bg) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 12.dp)) {
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
