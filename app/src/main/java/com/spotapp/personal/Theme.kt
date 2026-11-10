package com.spotapp.personal

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// The ten color themes, each in a paper (light) and an evening (dark) form. Same values as the iPhone app.

class SpotColors(
    val bg: Color, val card: Color, val chip: Color, val line: Color, val edge: Color,
    val ink: Color, val muted: Color, val accent: Color, val onAccent: Color, val accentText: Color,
    val cue: Color, val ok: Color,
    val nowEdge: Color, // outline of the set being edited
    val nowFill: Color, // fill of the number badge on that set
    val onNowFill: Color,
    val isDark: Boolean,
)

class PaletteInfo(val key: String, val name: String, val word: String)

val PALETTES = listOf(
    PaletteInfo("lagoon", "Lagoon", "teal blue green"),
    PaletteInfo("citrus", "Citrus", "lime yellow green"),
    PaletteInfo("moss", "Moss", "green"),
    PaletteInfo("sky", "Sky", "blue"),
    PaletteInfo("orange", "Orange", "orange"),
    PaletteInfo("terracotta", "Terracotta", "clay brown rust"),
    PaletteInfo("lilac", "Lilac", "purple violet"),
    PaletteInfo("rose", "Rose", "pink"),
    PaletteInfo("cherry", "Cherry", "red"),
    PaletteInfo("gold", "Gold", "yellow honey"),
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

private fun make(dark: Boolean, bg: Long, card: Long, chip: Long, line: Long, edge: Long, ink: Long, muted: Long, accent: Long, onAccent: Long, accentText: Long, cue: Long, ok: Long, nowEdge: Long, nowFill: Long, onNowFill: Long) = SpotColors(
    bg = c(bg), card = c(card), chip = c(chip), line = c(line), edge = c(edge), ink = c(ink), muted = c(muted), accent = c(accent), onAccent = c(onAccent), accentText = c(accentText), cue = c(cue), ok = c(ok), nowEdge = c(nowEdge), nowFill = c(nowFill), onNowFill = c(onNowFill), isDark = dark,
)

private val TABLE: Map<String, Pair<SpotColors, SpotColors>> = mapOf(
    "citrus" to Pair(
        make(false, 0xF6F7F2, 0xFFFFFF, 0xECEFE6, 0xE0E4D8, 0x7C8472, 0x151A14, 0x566052, 0xC6EE3A, 0x151A14, 0x3B5C0A, 0xEAF6BE, 0x2C6B3A, 0x151A14, 0xC6EE3A, 0x151A14),
        make(true, 0x0F1410, 0x182019, 0x222C23, 0x2B362C, 0x7F8B7B, 0xF1F5EA, 0xAEB8A8, 0xC6EE3A, 0x151A14, 0xC6EE3A, 0x2A3410, 0x7FD08F, 0xC6EE3A, 0xC6EE3A, 0x151A14),
    ),
    "lagoon" to Pair(
        make(false, 0xF4F1EA, 0xFFFFFF, 0xE9E6DC, 0xDDD9CC, 0x7A7F76, 0x0F2A2B, 0x4D6061, 0x0F5E5C, 0xF4F1EA, 0x0F5E5C, 0xFBE5D3, 0x3A7D32, 0xC2571A, 0xF0803C, 0x1B1305),
        make(true, 0x0B1A1B, 0x12292A, 0x1A3738, 0x234546, 0x6F9A97, 0xE8F2EE, 0xA3BDB9, 0x5CC9BD, 0x0B1A1B, 0x5CC9BD, 0x3D2A1D, 0x8BD17F, 0xF0803C, 0xF0803C, 0x1B1305),
    ),
    "moss" to Pair(
        make(false, 0xF4F7F3, 0xFFFFFF, 0xE7EDE3, 0xD9E2D5, 0x6C7B65, 0x172211, 0x495742, 0x4F7A28, 0xFFFFFF, 0x4F7A28, 0xE0F6D5, 0x2C6B3A, 0x172211, 0x4F7A28, 0xFFFFFF),
        make(true, 0x10180C, 0x1A2315, 0x263220, 0x32402B, 0x809376, 0xEEF4EB, 0xB4C2AE, 0x9BD05A, 0x10180C, 0x9BD05A, 0x253F18, 0x7FD08F, 0x9BD05A, 0x9BD05A, 0x10180C),
    ),
    "sky" to Pair(
        make(false, 0xF3F5F7, 0xFFFFFF, 0xE3E9ED, 0xD5DCE2, 0x65727B, 0x111B22, 0x424E57, 0x1F6FB5, 0xFFFFFF, 0x1F6FB5, 0xD5E8F6, 0x2C6B3A, 0x111B22, 0x1F6FB5, 0xFFFFFF),
        make(true, 0x0C1318, 0x151D23, 0x202A32, 0x2B3740, 0x768793, 0xEBF0F4, 0xAEB9C2, 0x6CB6F0, 0x0C1318, 0x6CB6F0, 0x182F3F, 0x7FD08F, 0x6CB6F0, 0x6CB6F0, 0x0C1318),
    ),
    "orange" to Pair(
        make(false, 0xF7F4F3, 0xFFFFFF, 0xEDE7E3, 0xE2DAD5, 0x7B6E65, 0x221811, 0x574A42, 0xD9692B, 0x221811, 0xB25623, 0xF6E2D5, 0x2C6B3A, 0x221811, 0xD9692B, 0x221811),
        make(true, 0x18110C, 0x231B15, 0x322720, 0x40332B, 0x938276, 0xF4EFEB, 0xC2B6AE, 0xF0954F, 0x18110C, 0xF0954F, 0x3F2718, 0x7FD08F, 0xF0954F, 0xF0954F, 0x18110C),
    ),
    "terracotta" to Pair(
        make(false, 0xF7F4F3, 0xFFFFFF, 0xEDE6E3, 0xE2D8D5, 0x7B6A65, 0x221511, 0x574742, 0xB4532F, 0xFFFFFF, 0xB4532F, 0xF6DDD5, 0x2C6B3A, 0x221511, 0xB4532F, 0xFFFFFF),
        make(true, 0x180F0C, 0x231815, 0x322420, 0x40302B, 0x937D76, 0xF4EDEB, 0xC2B2AE, 0xE08A66, 0x180F0C, 0xE08A66, 0x3F2118, 0x7FD08F, 0xE08A66, 0xE08A66, 0x180F0C),
    ),
    "lilac" to Pair(
        make(false, 0xF5F3F7, 0xFFFFFF, 0xE8E3ED, 0xDBD5E2, 0x6F657B, 0x191122, 0x4C4257, 0x7A56C2, 0xFFFFFF, 0x7A56C2, 0xE4D5F6, 0x2C6B3A, 0x191122, 0x7A56C2, 0xFFFFFF),
        make(true, 0x110C18, 0x1C1523, 0x282032, 0x352B40, 0x847693, 0xEFEBF4, 0xB7AEC2, 0xB79BEE, 0x110C18, 0xB79BEE, 0x2A183F, 0x7FD08F, 0xB79BEE, 0xB79BEE, 0x110C18),
    ),
    "rose" to Pair(
        make(false, 0xF7F3F4, 0xFFFFFF, 0xEDE3E6, 0xE2D5D8, 0x7B656B, 0x221115, 0x574247, 0xC0405E, 0xFFFFFF, 0xC0405E, 0xF6D5DD, 0x2C6B3A, 0x221115, 0xC0405E, 0xFFFFFF),
        make(true, 0x180C0F, 0x231518, 0x322024, 0x402B30, 0x93767D, 0xF4EBED, 0xC2AEB3, 0xF08AA3, 0x180C0F, 0xF08AA3, 0x3F1822, 0x7FD08F, 0xF08AA3, 0xF08AA3, 0x180C0F),
    ),
    "cherry" to Pair(
        make(false, 0xF7F3F3, 0xFFFFFF, 0xEDE3E4, 0xE2D5D6, 0x7B6567, 0x221112, 0x574244, 0xB3202E, 0xFFFFFF, 0xB3202E, 0xF6D5D8, 0x2C6B3A, 0x221112, 0xB3202E, 0xFFFFFF),
        make(true, 0x180C0D, 0x231516, 0x322021, 0x402B2D, 0x937678, 0xF4EBEC, 0xC2AEAF, 0xEE6A74, 0x180C0D, 0xEE6A74, 0x3F181B, 0x7FD08F, 0xEE6A74, 0xEE6A74, 0x180C0D),
    ),
    "gold" to Pair(
        make(false, 0xF7F6F3, 0xFFFFFF, 0xEDEAE3, 0xE2DED5, 0x7B7565, 0x221D11, 0x575142, 0xE0A33A, 0x221D11, 0x8F6825, 0xF6ECD5, 0x2C6B3A, 0x221D11, 0xE0A33A, 0x221D11),
        make(true, 0x18140C, 0x231F15, 0x322C20, 0x403A2B, 0x938A76, 0xF4F2EB, 0xC2BCAE, 0xF0C35A, 0x18140C, 0xF0C35A, 0x3F3318, 0x7FD08F, 0xF0C35A, 0xF0C35A, 0x18140C),
    ),
)

// theme: paper | evening | system. palette: one of the ten names above.
fun resolveColors(theme: String, palette: String, systemDark: Boolean): SpotColors {
    val dark = when (theme) { "evening" -> true; "paper" -> false; else -> systemDark }
    val pair = TABLE[palette] ?: TABLE.getValue("lagoon")
    return if (dark) pair.second else pair.first
}

val LocalColors = staticCompositionLocalOf { resolveColors("paper", "lagoon", false) }
val Serif: FontFamily = FontFamily.Serif
