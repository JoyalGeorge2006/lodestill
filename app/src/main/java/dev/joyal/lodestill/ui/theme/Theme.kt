package dev.joyal.lodestill.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.joyal.lodestill.R

// Vercel/Geist tokens inverted to true black (AMOLED). AMOLED redesign v0.2.0.
object HfColors {
    // Dark (AMOLED)
    val DarkBg = Color(0xFF000000); val DarkSurface = Color(0xFF0B0B0B)
    val DarkSurface2 = Color(0xFF101010)
    val DarkHair = Color(0xFF232323); val DarkHair2 = Color(0xFF2E2E2E)
    val DarkText = Color(0xFFF5F5F5); val DarkT2 = Color(0xFFA3A3A3); val DarkT3 = Color(0xFF666666)
    val DarkRingTrack = Color(0xFF1C1C1C); val DarkScaffold = Color(0xFF6E6E6E)
    val Green = Color(0xFF3FB27F)
    val DarkErr = Color(0xFFFF7A8A); val DarkWarn = Color(0xFFF2C00B)
    // Light
    val LightBg = Color(0xFFFAFAFA); val LightSurface = Color(0xFFFFFFFF)
    val LightSurface2 = Color(0xFFF5F5F5)
    val LightHair = Color(0xFFEBEBEB); val LightHair2 = Color(0xFFDEDEDE)
    val LightText = Color(0xFF171717); val LightT2 = Color(0xFF666666); val LightT3 = Color(0xFF999999)
    val LightRingTrack = Color(0xFFE8E8E8); val LightScaffold = Color(0xFFABABAB)
    val LightErr = Color(0xFFC21030); val LightWarn = Color(0xFF8A6500)
}

val GeistSans = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold)
)
val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
    Font(R.font.geist_mono_semibold, FontWeight.SemiBold)
)

data class HfExtra(
    val hair: Color, val hair2: Color, val muted: Color, val surface2: Color,
    val ringTrack: Color, val scaffold: Color, val ok: Color, val warn: Color, val err: Color
)
val LocalHf = staticCompositionLocalOf {
    HfExtra(HfColors.LightHair, HfColors.LightHair2, HfColors.LightT3, HfColors.LightSurface2,
        HfColors.LightRingTrack, HfColors.LightScaffold, HfColors.Green, HfColors.LightWarn, HfColors.LightErr)
}

private val DarkScheme = darkColorScheme(
    primary = HfColors.DarkText, onPrimary = Color(0xFF000000),
    background = HfColors.DarkBg, onBackground = HfColors.DarkText,
    surface = HfColors.DarkSurface, onSurface = HfColors.DarkText,
    surfaceVariant = HfColors.DarkSurface2, onSurfaceVariant = HfColors.DarkT2,
    outline = HfColors.DarkHair, outlineVariant = HfColors.DarkHair2,
    error = HfColors.DarkErr
)
private val LightScheme = lightColorScheme(
    primary = HfColors.LightText, onPrimary = Color(0xFFFFFFFF),
    background = HfColors.LightBg, onBackground = HfColors.LightText,
    surface = HfColors.LightSurface, onSurface = HfColors.LightText,
    surfaceVariant = HfColors.LightSurface2, onSurfaceVariant = HfColors.LightT2,
    outline = HfColors.LightHair, outlineVariant = HfColors.LightHair2,
    error = HfColors.LightErr
)
private val HfShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
)
private val HfType = Typography(
    headlineLarge = TextStyle(fontFamily = GeistSans, fontWeight = FontWeight.Medium,
        fontSize = 27.sp, lineHeight = 30.sp, letterSpacing = (-0.9).sp),
    headlineSmall = TextStyle(fontFamily = GeistSans, fontWeight = FontWeight.Medium,
        fontSize = 27.sp, lineHeight = 30.sp, letterSpacing = (-0.9).sp),
    titleLarge = TextStyle(fontFamily = GeistSans, fontWeight = FontWeight.Medium,
        fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.7).sp),
    titleMedium = TextStyle(fontFamily = GeistSans, fontWeight = FontWeight.Medium, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = GeistSans, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = GeistSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = GeistSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = GeistSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelMedium = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 11.sp)
)

@Composable
fun LodestillTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val extra = if (dark)
        HfExtra(HfColors.DarkHair, HfColors.DarkHair2, HfColors.DarkT3, HfColors.DarkSurface2,
            HfColors.DarkRingTrack, HfColors.DarkScaffold, HfColors.Green, HfColors.DarkWarn, HfColors.DarkErr)
    else
        HfExtra(HfColors.LightHair, HfColors.LightHair2, HfColors.LightT3, HfColors.LightSurface2,
            HfColors.LightRingTrack, HfColors.LightScaffold, HfColors.Green, HfColors.LightWarn, HfColors.LightErr)
    CompositionLocalProvider(LocalHf provides extra) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme,
            shapes = HfShapes, typography = HfType, content = content)
    }
}
