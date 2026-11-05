package com.spotapp.personal

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

data class SpotColors(
    val bg: Color, val card: Color, val chip: Color, val line: Color, val edge: Color,
    val ink: Color, val muted: Color, val accent: Color, val onAccent: Color, val accentText: Color,
    val cue: Color, val ok: Color,
    val nowEdge: Color, // outline of the set being edited
    val nowFill: Color, // fill of the number badge on that set
    val onNowFill: Color,
    val isDark: Boolean,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

private val CITRUS_PAPER = SpotColors(
    bg = c(0xF6F7F2), card = c(0xFFFFFF), chip = c(0xECEFE6), line = c(0xE0E4D8), edge = c(0x7C8472),
    ink = c(0x151A14), muted = c(0x566052), accent = c(0xC6EE3A), onAccent = c(0x151A14), accentText = c(0x3B5C0A),
    cue = c(0xEAF6BE), ok = c(0x2C6B3A), nowEdge = c(0x151A14), nowFill = c(0xC6EE3A), onNowFill = c(0x151A14), isDark = false,
)
private val CITRUS_EVENING = SpotColors(
    bg = c(0x0F1410), card = c(0x182019), chip = c(0x222C23), line = c(0x2B362C), edge = c(0x7F8B7B),
    ink = c(0xF1F5EA), muted = c(0xAEB8A8), accent = c(0xC6EE3A), onAccent = c(0x151A14), accentText = c(0xC6EE3A),
    cue = c(0x2A3410), ok = c(0x7FD08F), nowEdge = c(0xC6EE3A), nowFill = c(0xC6EE3A), onNowFill = c(0x151A14), isDark = true,
)
private val LAGOON_PAPER = SpotColors(
    bg = c(0xF4F1EA), card = c(0xFFFFFF), chip = c(0xE9E6DC), line = c(0xDDD9CC), edge = c(0x7A7F76),
    ink = c(0x0F2A2B), muted = c(0x4D6061), accent = c(0x0F5E5C), onAccent = c(0xF4F1EA), accentText = c(0x0F5E5C),
    cue = c(0xFBE5D3), ok = c(0x3A7D32), nowEdge = c(0xC2571A), nowFill = c(0xF0803C), onNowFill = c(0x1B1305), isDark = false,
)
private val LAGOON_EVENING = SpotColors(
    bg = c(0x0B1A1B), card = c(0x12292A), chip = c(0x1A3738), line = c(0x234546), edge = c(0x6F9A97),
    ink = c(0xE8F2EE), muted = c(0xA3BDB9), accent = c(0x5CC9BD), onAccent = c(0x0B1A1B), accentText = c(0x5CC9BD),
    cue = c(0x3D2A1D), ok = c(0x8BD17F), nowEdge = c(0xF0803C), nowFill = c(0xF0803C), onNowFill = c(0x1B1305), isDark = true,
)

// theme: paper | evening | system. palette: citrus | lagoon. Same values as the iPhone app.
fun resolveColors(theme: String, palette: String, systemDark: Boolean): SpotColors {
    val dark = when (theme) { "evening" -> true; "paper" -> false; else -> systemDark }
    return if (palette == "citrus") (if (dark) CITRUS_EVENING else CITRUS_PAPER) else (if (dark) LAGOON_EVENING else LAGOON_PAPER)
}

val LocalColors = staticCompositionLocalOf { LAGOON_PAPER }
val Serif: FontFamily = FontFamily.Serif

@Composable
fun systemDark(): Boolean = isSystemInDarkTheme()
