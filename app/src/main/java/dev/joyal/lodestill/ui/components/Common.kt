package dev.joyal.lodestill.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.joyal.lodestill.ui.theme.GeistMono
import dev.joyal.lodestill.ui.theme.GeistSans
import dev.joyal.lodestill.ui.theme.LocalHf
import java.util.Locale

// Monospace uppercase eyebrow (Geist Mono 10sp, tracked, secondary color)
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(Locale.US), modifier = modifier, fontFamily = GeistMono,
        fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 0.8.sp, color = color)
}

// Mono section label inside cards (muted). Pass already-uppercase text; never
// transform case here because UI tests match some of these strings.
@Composable
fun MLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontFamily = GeistMono,
        fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 0.8.sp,
        color = LocalHf.current.muted)
}

// Mono value on the right side of rows
@Composable
fun MonoVal(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, modifier = modifier, fontFamily = GeistMono, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, color = color)
}

@Composable
fun HfCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
        .border(1.dp, LocalHf.current.hair, RoundedCornerShape(6.dp))
        .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
fun HfCard(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
        .border(1.dp, LocalHf.current.hair, RoundedCornerShape(6.dp))
        .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
fun HfGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
        .border(1.dp, LocalHf.current.hair, RoundedCornerShape(6.dp))
        .padding(horizontal = 14.dp), content = content)
}

@Composable
fun HfDivider() = HorizontalDivider(color = LocalHf.current.hair, thickness = 1.dp)

@Composable
fun HfRow(
    icon: (@Composable () -> Unit)? = null, title: String, sub: String? = null,
    end: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null
) {
    val m = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
    Row(if (onClick != null) m.clickable(onClick = onClick) else m,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) { icon() }
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall,
                color = LocalHf.current.muted)
        }
        end?.invoke()
    }
}

@Composable
fun HfChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (selected) MaterialTheme.colorScheme.primary else LocalHf.current.hair2
    Surface(onClick = onClick, shape = CircleShape, color = bg,
        border = BorderStroke(1.dp, border)) {
        Text(text.uppercase(java.util.Locale.US), Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontFamily = GeistMono, fontSize = 10.sp, letterSpacing = 0.6.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = fg)
    }
}

@Composable
fun HfSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()
        .border(1.dp, LocalHf.current.hair, RoundedCornerShape(6.dp))) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Surface(onClick = { onSelect(i) }, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(5.dp),
                color = if (on) MaterialTheme.colorScheme.primary else Color.Transparent) {
                Box(Modifier.fillMaxWidth().padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text(o.uppercase(java.util.Locale.US), fontFamily = GeistMono, fontSize = 10.sp,
                        letterSpacing = 0.7.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (on) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun HfPrimary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: (@Composable () -> Unit)? = null) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(40.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary),
        contentPadding = PaddingValues(horizontal = 16.dp)) {
        icon?.invoke(); Spacer(if (icon != null) Modifier.width(8.dp) else Modifier.width(0.dp))
        Text(text, fontFamily = GeistSans, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HfSecondary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: (@Composable () -> Unit)? = null) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, LocalHf.current.hair2),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = 16.dp)) {
        icon?.invoke(); Spacer(if (icon != null) Modifier.width(8.dp) else Modifier.width(0.dp))
        Text(text, fontFamily = GeistSans, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// Stacked layer visual: solid = built layers, dashed = scaffolding
@Composable
fun LayerStack(layers: Int, scaffolding: Int = 0, modifier: Modifier = Modifier,
               layerH: Dp = 9.dp, gap: Dp = 3.dp, maxW: Dp = 120.dp, accentLast: Boolean = false) {
    val accent = LocalHf.current.ok
    val block = MaterialTheme.colorScheme.onSurface
    val dash = LocalHf.current.scaffold
    val count = (layers + scaffolding).coerceAtLeast(1)
    val h = (layerH + gap) * count + 4.dp
    Canvas(modifier.size(width = maxW, height = h)) {
        var y = size.height
        if (layers == 0 && scaffolding == 0) {
            y -= (layerH + gap).toPx()
            drawRoundRect(dash.copy(alpha = 0.5f), Offset(0f, y), Size(size.width, layerH.toPx()),
                androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
        }
        for (i in 0 until layers) {
            y -= (layerH + gap).toPx()
            drawRoundRect(if (accentLast && i == layers - 1) accent else block,
                Offset(0f, y), Size(size.width, layerH.toPx()),
                androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        }
        for (j in 0 until scaffolding) {
            y -= (layerH + gap).toPx()
            drawRoundRect(dash, Offset(0f, y), Size(size.width, layerH.toPx()),
                androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
        }
    }
}

// Timer ring: thin hairline track, white progress, mono digits centered
@Composable
fun TimerRing(progress: Float, label: String, sub: String, modifier: Modifier = Modifier, size: Dp = 236.dp) {
    val fg = MaterialTheme.colorScheme.onSurface
    val track = LocalHf.current.ringTrack
    val muted = LocalHf.current.muted
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val swTrack = 2.dp.toPx(); val swProg = 3.dp.toPx()
            val inset = swProg
            drawArc(track, 0f, 360f, false, Offset(inset, inset),
                Size(this.size.width - 2 * inset, this.size.height - 2 * inset),
                style = Stroke(swTrack))
            drawArc(fg, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset),
                Size(this.size.width - 2 * inset, this.size.height - 2 * inset),
                style = Stroke(swProg, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontFamily = GeistMono, fontWeight = FontWeight.Medium, fontSize = 44.sp,
                letterSpacing = (-1).sp, color = fg, fontFeatureSettings = "tnum")
            Text(sub.uppercase(Locale.US), fontFamily = GeistMono, fontWeight = FontWeight.Normal,
                fontSize = 10.sp, letterSpacing = 1.2.sp, color = muted)
        }
    }
}

fun fmtClock(sec: Long): String = "%d:%02d".format(sec / 60, sec % 60)
fun fmtDur(sec: Long): String {
    val h = sec / 3600; val m = (sec % 3600) / 60
    return if (h > 0) "${h}h %02dm".format(m) else "${m}m"
}
