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

/** An evening around the room: dusk indigo for the walls of the app, cream paper, brass, and lingonberry red. */
@Immutable
object C {
    val wood = Color(0xFF1E2240)
    val woodD = Color(0xFF151830)
    val woodL = Color(0xFF2E3560)
    val night = Color(0xFF0C0E1E)
    val ink = Color(0xFF1A1A30)
    val paper = Color(0xFFF4ECDA)
    val paperD = Color(0xFFE2D6BC)
    val note = Color(0xFFFFF6E0)
    val muted = Color(0xFF7E86B0)
    val brass = Color(0xFFF2C14E)
    val brassL = Color(0xFFFFF0A8)
    val brassD = Color(0xFFB07E1C)
    val crim = Color(0xFFE0605A)
    val crimD = Color(0xFF9A3036)
    val crimL = Color(0xFFF59088)

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
