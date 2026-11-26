package dev.joyal.lodestill.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.joyal.lodestill.ui.theme.LocalHf

@Composable
fun HfCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun HfCard(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
        .clickable(onClick = onClick).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun HfGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)), content = content)
}

@Composable
fun HfDivider() = HorizontalDivider(color = LocalHf.current.border, thickness = 1.dp)

@Composable
fun SecLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 2.dp))
}

@Composable
fun HfRow(
    icon: (@Composable () -> Unit)? = null, title: String, sub: String? = null,
    end: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null
) {
    val m = Modifier.fillMaxWidth().defaultMinSize(minHeight = 54.dp)
    Row(
        if (onClick != null) m.then(Modifier).let { it } else m,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) { icon() }
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (sub != null) Text(sub, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        end?.invoke()
    }
}

@Composable
fun HfChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val border = if (selected) Color.Transparent else LocalHf.current.track
    Surface(onClick = onClick, shape = CircleShape, color = bg,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, border)) {
        Text(text, Modifier.padding(horizontal = 15.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium, color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
fun HfSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(LocalHf.current.neutral, RoundedCornerShape(22.dp)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Surface(onClick = { onSelect(i) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(19.dp),
                color = if (on) MaterialTheme.colorScheme.surface else Color.Transparent) {
                Box(Modifier.fillMaxWidth().padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                    Text(o, style = MaterialTheme.typography.bodyMedium,
                        color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun HfPrimary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: (@Composable () -> Unit)? = null) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(48.dp), shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
        icon?.invoke(); Spacer(if (icon != null) Modifier.width(8.dp) else Modifier.width(0.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HfSecondary(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: (@Composable () -> Unit)? = null) {
    Button(onClick = onClick, modifier = modifier.height(48.dp), shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = LocalHf.current.neutral,
            contentColor = MaterialTheme.colorScheme.onSurface)) {
        icon?.invoke(); Spacer(if (icon != null) Modifier.width(8.dp) else Modifier.width(0.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HfTag(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = color,
        modifier = Modifier.background(LocalHf.current.neutral, RoundedCornerShape(9.dp))
            .padding(horizontal = 9.dp, vertical = 2.dp))
}

// Stacked layer visual: solid = built layers, dashed = scaffolding
@Composable
fun LayerStack(layers: Int, scaffolding: Int = 0, modifier: Modifier = Modifier,
               layerH: Dp = 11.dp, gap: Dp = 3.dp, maxW: Dp = 96.dp, accentLast: Boolean = false) {
    val accent = MaterialTheme.colorScheme.primary
    val block = LocalHf.current.block; val track = LocalHf.current.track
    val dash = MaterialTheme.colorScheme.onSurfaceVariant
    val h = (layerH + gap) * (layers + scaffolding) + 8.dp
    Canvas(modifier.size(width = maxW, height = h)) {
        val bw = size.width - 16
        drawRoundRect(track, Offset(4f, size.height - 5f), Size(size.width - 8, 4f), CornerRadius(2f))
        var y = size.height - 6f
        for (i in 0 until layers) {
            val ww = bw - (if (i < 6) i * 5 else 30).dp.toPx()
            y -= (layerH + gap).toPx()
            drawRoundRect(if (accentLast && i == layers - 1) accent else block,
                Offset((size.width - ww) / 2, y), Size(ww, layerH.toPx()), CornerRadius(2.5.dp.toPx()))
        }
        for (j in 0 until scaffolding) {
            val i = layers + j
            val ww = bw - (if (i < 6) i * 5 else 30).dp.toPx()
            y -= (layerH + gap).toPx()
            drawRoundRect(dash, Offset((size.width - ww) / 2, y), Size(ww, layerH.toPx()), CornerRadius(2.5.dp.toPx()),
                style = Stroke(width = 1.4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
        }
    }
}

// Timer ring with remaining time centered
@Composable
fun TimerRing(progress: Float, label: String, sub: String, modifier: Modifier = Modifier, size: Dp = 200.dp) {
    val accent = MaterialTheme.colorScheme.primary; val track = LocalHf.current.track
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = 8.dp.toPx(); val inset = sw / 2
            drawArc(track.copy(alpha = 0.4f), 0f, 360f, false, Offset(inset, inset),
                Size(this.size.width - sw, this.size.height - sw), style = Stroke(sw))
            drawArc(accent, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset),
                Size(this.size.width - sw, this.size.height - sw), style = Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp,
                fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface)
            Text(sub, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun fmtClock(sec: Long): String = "%d:%02d".format(sec / 60, sec % 60)
fun fmtDur(sec: Long): String {
    val h = sec / 3600; val m = (sec % 3600) / 60
    return if (h > 0) "${h} h ${m} min".replace(" 0 min", "") else "$m min"
}
