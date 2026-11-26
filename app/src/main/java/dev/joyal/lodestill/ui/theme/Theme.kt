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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Approved v3 minimal Meta tokens
object HfColors {
    val LightBody = Color(0xFFF0F2F5); val LightSurface = Color(0xFFFFFFFF)
    val LightText = Color(0xFF0A1317); val LightTextSec = Color(0xFF5A6B7A)
    val LightAccent = Color(0xFF0064E0); val LightNeutral = Color(0x12053659)
    val LightBorder = Color(0x14053659); val LightTrack = Color(0xFFDDE3E9)
    val LightBlock = Color(0xFFB7C1CB); val LightWarn = Color(0xFF8A6500)
    val LightErr = Color(0xFFC21030); val LightOk = Color(0xFF0D8626)
    val DarkBody = Color(0xFF111112); val DarkSurface = Color(0xFF1C1C1F)
    val DarkText = Color(0xFFDFE2E5); val DarkTextSec = Color(0xFF9AA1A9)
    val DarkAccent = Color(0xFF0171E3); val DarkAccentT = Color(0xFF3E9EFB)
    val DarkNeutral = Color(0x1FDFE2E5); val DarkBorder = Color(0x12F2F4F6)
    val DarkTrack = Color(0xFF3E4248); val DarkBlock = Color(0xFF5A5E66)
    val DarkWarn = Color(0xFFF2C00B); val DarkErr = Color(0xFFFF7A8A)
    val DarkOk = Color(0xFF2FA84A)
}

data class HfExtra(
    val track: Color, val block: Color, val warn: Color, val err: Color, val ok: Color,
    val neutral: Color, val border: Color, val accentT: Color
)
val LocalHf = staticCompositionLocalOf {
    HfExtra(HfColors.LightTrack, HfColors.LightBlock, HfColors.LightWarn, HfColors.LightErr,
        HfColors.LightOk, HfColors.LightNeutral, HfColors.LightBorder, HfColors.LightAccent)
}

private val LightScheme = lightColorScheme(
    primary = HfColors.LightAccent, onPrimary = Color.White,
    background = HfColors.LightBody, onBackground = HfColors.LightText,
    surface = HfColors.LightSurface, onSurface = HfColors.LightText,
    onSurfaceVariant = HfColors.LightTextSec,
    surfaceVariant = HfColors.LightSurface,
    outline = HfColors.LightBorder, outlineVariant = HfColors.LightTrack,
    error = HfColors.LightErr
)
private val DarkScheme = darkColorScheme(
    primary = HfColors.DarkAccent, onPrimary = Color.White,
    background = HfColors.DarkBody, onBackground = HfColors.DarkText,
    surface = HfColors.DarkSurface, onSurface = HfColors.DarkText,
    onSurfaceVariant = HfColors.DarkTextSec,
    surfaceVariant = HfColors.DarkSurface,
    outline = HfColors.DarkBorder, outlineVariant = HfColors.DarkTrack,
    error = HfColors.DarkErr
)
private val HfShapes = Shapes(medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
private val HfType = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun LodestillTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val extra = if (dark) HfExtra(HfColors.DarkTrack, HfColors.DarkBlock, HfColors.DarkWarn, HfColors.DarkErr,
        HfColors.DarkOk, HfColors.DarkNeutral, HfColors.DarkBorder, HfColors.DarkAccentT)
    else HfExtra(HfColors.LightTrack, HfColors.LightBlock, HfColors.LightWarn, HfColors.LightErr,
        HfColors.LightOk, HfColors.LightNeutral, HfColors.LightBorder, HfColors.LightAccent)
    CompositionLocalProvider(LocalHf provides extra) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, shapes = HfShapes, typography = HfType, content = content)
    }
}
