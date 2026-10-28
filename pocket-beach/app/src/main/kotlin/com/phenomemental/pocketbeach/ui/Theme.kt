package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.phenomemental.pocketbeach.R

/**
 * The book look: warm paper, near-black ink, a framed sheet for what a block
 * says, and one crimson accent. The dark version keeps the feel: dark paper,
 * warm light ink, the same crimson brightened to read on it.
 */
@Immutable
class Ink(
    val paper: Color,
    val sheet: Color,
    val ink: Color,
    val soft: Color,
    val rule: Color,
    val crimson: Color,
    val wash: Color,
    val dark: Boolean,
)

val PaperInk = Ink(
    paper = Color(0xFFF4EEE3),
    sheet = Color(0xFFFBF8F1),
    ink = Color(0xFF1C1A17),
    soft = Color(0xFF6A6155),
    rule = Color(0xFFD5C8B3),
    crimson = Color(0xFF9B1B30),
    wash = Color(0xFFF3E3E0),
    dark = false,
)

val NightInk = Ink(
    paper = Color(0xFF16130F),
    sheet = Color(0xFF1F1B16),
    ink = Color(0xFFEAE2D4),
    soft = Color(0xFFA89D8C),
    rule = Color(0xFF4A4136),
    crimson = Color(0xFFDB6474),
    wash = Color(0xFF2E1B1C),
    dark = true,
)

val LocalInk = staticCompositionLocalOf { PaperInk }

private fun cormorant(weight: Int, italic: Boolean = false) = Font(
    if (italic) R.font.cormorant_garamond_italic else R.font.cormorant_garamond,
    FontWeight(weight),
    if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Cormorant Garamond, bundled (SIL Open Font License), at every weight the app uses. */
val Cormorant = FontFamily(
    cormorant(400), cormorant(500), cormorant(600), cormorant(700),
    cormorant(400, italic = true), cormorant(500, italic = true), cormorant(600, italic = true),
)

/** The type: generous sizes for reading at arm's length, the small labels set wide in capitals. */
object Type {
    val title = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 40.sp)
    val heading = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 27.sp, lineHeight = 34.sp)
    val body = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Normal, fontSize = 21.sp, lineHeight = 30.sp)
    val name = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp)
    val aside = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Italic, fontSize = 19.sp, lineHeight = 26.sp)
    val small = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 23.sp)
    val label = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.16.em)
    val address = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 30.sp)
    val action = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 22.sp, letterSpacing = 0.01.em)
    val call = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp)
}

@Composable
fun PocketTheme(content: @Composable () -> Unit) {
    val ink = if (isSystemInDarkTheme()) NightInk else PaperInk
    val scheme = if (ink.dark) {
        darkColorScheme(primary = ink.crimson, onPrimary = ink.paper, background = ink.paper, onBackground = ink.ink, surface = ink.paper, onSurface = ink.ink, outline = ink.rule, surfaceVariant = ink.sheet, onSurfaceVariant = ink.soft)
    } else {
        lightColorScheme(primary = ink.crimson, onPrimary = ink.paper, background = ink.paper, onBackground = ink.ink, surface = ink.paper, onSurface = ink.ink, outline = ink.rule, surfaceVariant = ink.sheet, onSurfaceVariant = ink.soft)
    }
    CompositionLocalProvider(LocalInk provides ink) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
