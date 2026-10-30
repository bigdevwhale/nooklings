package app.nooklings.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.nooklings.R

/** The room's own materials: dark wood, cream paper, brass, and the red of the gingham on the jar lids. */
@Immutable
object C {
    val wood = Color(0xFF4A2C1A)
    val woodD = Color(0xFF2C180E)
    val woodL = Color(0xFF6A4228)
    val night = Color(0xFF140C08)
    val ink = Color(0xFF2A160E)
    val paper = Color(0xFFF6EAD0)
    val paperD = Color(0xFFE2CFA6)
    val note = Color(0xFFFFF6E0)
    val muted = Color(0xFF80624A)
    val brass = Color(0xFFE8B840)
    val brassL = Color(0xFFFFF0A0)
    val brassD = Color(0xFFA87418)
    val crim = Color(0xFFC2402F)
    val crimD = Color(0xFF7E2418)
    val crimL = Color(0xFFF0806C)

    fun of(rgb: Int) = Color(0xFF000000 or (rgb.toLong() and 0xFFFFFF))
}

/** m6x11 by Daniel Linssen with Cyrillic by Natto Games (free with attribution): one family, sizes set the hierarchy. */
val Pixel = FontFamily(Font(R.font.m6x11, FontWeight.Normal))

/** Marck Script by Denis Masharov (SIL OFL): the handwriting on jar labels. */
val Hand = FontFamily(Font(R.font.marck_script, FontWeight.Normal))

object T {
    val Display = TextStyle(fontFamily = Pixel, fontSize = 32.sp, lineHeight = 31.sp, color = C.ink)
    val Title = TextStyle(fontFamily = Pixel, fontSize = 27.sp, lineHeight = 27.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Pixel, fontSize = 23.sp, lineHeight = 24.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Pixel, fontSize = 20.sp, lineHeight = 21.sp, color = C.ink)
    val Say = TextStyle(fontFamily = Pixel, fontSize = 18.sp, lineHeight = 20.sp, color = C.ink)
    val Small = TextStyle(fontFamily = Pixel, fontSize = 16.sp, lineHeight = 17.sp, color = C.muted)
    val Button = TextStyle(fontFamily = Pixel, fontSize = 20.sp, lineHeight = 20.sp)
    val Number = TextStyle(fontFamily = Pixel, fontSize = 48.sp, lineHeight = 46.sp, color = C.brass)
    val Label = TextStyle(fontFamily = Hand, fontSize = 26.sp, lineHeight = 27.sp, color = C.ink)
    val LabelSmall = TextStyle(fontFamily = Hand, fontSize = 18.sp, lineHeight = 19.sp, color = C.paper)
}

@Composable
fun NookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.brass, onPrimary = C.ink, background = C.wood, surface = C.paper, onSurface = C.ink,
            onBackground = C.paper, surfaceContainer = C.paper, outline = C.ink, error = C.crim,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
