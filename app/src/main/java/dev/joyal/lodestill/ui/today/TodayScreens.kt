package dev.joyal.lodestill.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavController
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.core.Engine
import dev.joyal.lodestill.data.Place
import dev.joyal.lodestill.ui.components.*
import dev.joyal.lodestill.ui.theme.GeistMono
import dev.joyal.lodestill.ui.theme.LocalHf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HfField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier,
            keyboard: KeyboardOptions = KeyboardOptions.Default, mono: Boolean = false, maxLines: Int = 1) {
    val c = MaterialTheme.colorScheme
    OutlinedTextField(value, onChange, modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = LocalHf.current.muted, fontSize = 14.sp) },
        singleLine = maxLines == 1, maxLines = maxLines, keyboardOptions = keyboard,
        shape = RoundedCornerShape(6.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp,
            fontFamily = if (mono) GeistMono else dev.joyal.lodestill.ui.theme.GeistSans, color = c.onSurface),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.onSurfaceVariant, unfocusedBorderColor = LocalHf.current.hair2,
            cursorColor = c.onSurface, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
}

@Composable
fun HfDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {},
        containerColor = LocalHf.current.surface2, shape = RoundedCornerShape(6.dp),
        modifier = Modifier.background(Color.Transparent),
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) })
}

@Composable
fun TodayScreen(vm: AppViewModel, nav: NavController) {
    val current by vm.current.collectAsState()
    val now by vm.now.collectAsState()
    val places by vm.places.collectAsState()
    val sessions by vm.allSessions.collectAsState()
    val scaffolding by vm.scaffolding.collectAsState()
    val settings by vm.settings.collectAsState()
    val timerOnly by vm.timerOnly.collectAsState()
    var showStart by remember { mutableStateOf(false) }
    var showEndEarly by remember { mutableStateOf(false) }
    val s0 = current

    // Timer-only mode exits automatically when the session is no longer actively running.
    LaunchedEffect(s0?.status, timerOnly) {
        if (timerOnly && s0?.status != "ACTIVE") vm.setTimerOnly(false)
    }

    if (timerOnly && s0 != null && s0.status == "ACTIVE") {
        TimerOnlyScreen(Engine.remainingSec(s0, now)) { vm.setTimerOnly(false) }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val s = current
        val date = SimpleDateFormat("EEEE d MMM", Locale.getDefault()).format(Date(now))
        if (s == null) {
            Eyebrow(date)
            Text("Ready to focus?", style = MaterialTheme.typography.headlineLarge)
            HfCard {
                MLabel("NEXT SESSION")
                Text("${settings.defaultLengthMin} minutes builds one layer at any place.",
                    style = MaterialTheme.typography.bodySmall, color = LocalHf.current.muted)
                HfPrimary("Start a session", onClick = { showStart = true })
            }
            TodaySummary(vm, places, sessions, 0L)
            if (scaffolding.isNotEmpty()) {
                HfCard(onClick = { nav.navigate("unfinished") }) {
                    MLabel("SCAFFOLDING")
                    HfRow(title = "${scaffolding.size} interrupted", sub = "Finish them to raise a layer",
                        end = { MonoVal("View  \u203A") })
                }
            }
        } else {
            val place = places.firstOrNull { it.id == s.placeId }
            when (s.status) {
                "ACTIVE" -> {
                    val remaining = Engine.remainingSec(s, now)
                    val progress = 1f - remaining.toFloat() / s.targetSec.toFloat()
                    Eyebrow("${place?.name ?: "Focus"} \u00B7 session active")
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        TimerRing(progress.coerceIn(0f, 1f), Engine.fmtRemaining(remaining), "remaining")
                        Spacer(Modifier.height(14.dp))
                        s.intent?.takeIf { it.isNotBlank() }?.let {
                            Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                        }
                        Eyebrow("${fmtClock(Engine.focusedSec(s, now))} done")
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HfSecondary("Pause", onClick = { vm.pause() }, modifier = Modifier.weight(1f))
                        HfSecondary("End early", onClick = { showEndEarly = true }, modifier = Modifier.weight(1f))
                    }
                    HfSecondary("Keep screen on", onClick = { vm.setTimerOnly(true) }, modifier = Modifier.fillMaxWidth())
                    Text("Keep screen on shows only the remaining time and stops the screen from sleeping while the session runs. Tap anywhere to return.",
                        style = MaterialTheme.typography.bodySmall, color = LocalHf.current.muted,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text("Leave the app? You have ${settings.graceSec} seconds to come back before the layer becomes scaffolding.",
                        style = MaterialTheme.typography.bodySmall, color = LocalHf.current.muted,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                "PAUSED" -> {
                    Eyebrow("${place?.name ?: "Focus"} \u00B7 paused")
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        TimerRing((s.accumulatedSec.toFloat() / s.targetSec).coerceIn(0f, 1f),
                            Engine.fmtRemaining(Engine.remainingSec(s, now)), "remaining")
                    }
                    HfPrimary("Resume", onClick = { vm.unpause() })
                    HfSecondary("End early", onClick = { showEndEarly = true }, modifier = Modifier.fillMaxWidth())
                }
                "INTERRUPTED" -> {
                    Eyebrow("Interrupted")
                    Text("Interrupted", style = MaterialTheme.typography.headlineLarge)
                    Text("${fmtClock(s.accumulatedSec)} of focus is kept as scaffolding. Finish it within ${settings.finishWindowH} hours and the layer still counts.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    HfPrimary("Continue session", onClick = { vm.resumeInterrupted() })
                    HfSecondary("Let it rest", onClick = { vm.endEarly() }, modifier = Modifier.fillMaxWidth())
                }
                "COMPLETED" -> {
                    val layers = vm.layersBuilt(s.placeId, sessions)
                    Eyebrow("Session complete")
                    Text("Layer built", style = MaterialTheme.typography.headlineLarge)
                    Text("${place?.name ?: "Your place"} grew by one layer. Nothing decays.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HfCard {
                        MLabel((place?.name ?: "Place").uppercase(Locale.US))
                        LayerStack(layers, maxW = 400.dp, accentLast = true, modifier = Modifier.fillMaxWidth())
                        Text("$layers layers", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TodaySummary(vm, places, sessions, 0L)
                    HfPrimary("Done", onClick = { vm.endEarly() })
                }
            }
            if (s.status != "COMPLETED") TodaySummary(vm, places, sessions, s.accumulatedSec)
        }
    }

    if (showStart) StartSheet(vm, places, settings.defaultLengthMin) { showStart = false }
    if (showEndEarly) {
        val kept = current?.accumulatedSec ?: 0
        HfDialog(onDismiss = { showEndEarly = false }) {
            Text("End early?", style = MaterialTheme.typography.titleMedium)
            Text("${fmtClock(kept)} of focus will be kept as scaffolding. Finish it within ${settings.finishWindowH} hours and the layer still counts.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            HfPrimary("Keep focusing", onClick = { showEndEarly = false })
            HfSecondary("End early", onClick = { vm.endEarly(); showEndEarly = false }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TodaySummary(vm: AppViewModel, places: List<Place>, sessions: List<dev.joyal.lodestill.data.FocusSession>, extra: Long) {
    val todaySec = vm.todayFocusSec(sessions) + extra
    val layersToday = places.sumOf { vm.layersBuilt(it.id, sessions) }
    HfCard {
        MLabel("TODAY'S FOCUS")
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(fmtDur(todaySec), fontFamily = GeistMono, fontWeight = FontWeight.Medium, fontSize = 36.sp,
                letterSpacing = (-0.7).sp, color = MaterialTheme.colorScheme.onSurface)
            Text("$layersToday layers total", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

// Opt-in keep-screen-on timer: black, digits only. Not the system always-on display.
@Composable
fun TimerOnlyScreen(remainingSec: Long, onExit: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val minute = (remainingSec / 60).toInt()
    val dx = (((minute * 37) % 41) - 20).dp   // drift against burn-in, changes each minute
    val dy = (((minute * 53) % 61) - 30).dp
    Box(Modifier.fillMaxSize().background(Color.Black)
        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null, onClick = onExit),
        contentAlignment = Alignment.Center) {
        Text(Engine.fmtRemaining(remainingSec), modifier = Modifier.offset(dx, dy),
            fontFamily = GeistMono, fontWeight = FontWeight.Medium, fontSize = 80.sp,
            letterSpacing = (-1.6).sp, color = Color(0xFFE8E8E8))
    }
}

@Composable
fun StartSheet(vm: AppViewModel, places: List<Place>, defaultMin: Int, onClose: () -> Unit) {
    var placeId by remember { mutableStateOf(places.firstOrNull()?.id ?: 0L) }
    var minutes by remember { mutableStateOf(defaultMin) }
    var custom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var intent by remember { mutableStateOf("") }
    HfDialog(onDismiss = onClose) {
        Eyebrow("New session")
        Text("Start a session", style = MaterialTheme.typography.titleMedium)
        MLabel("PLACE")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            places.forEach { p -> HfChip(p.name, p.id == placeId) { placeId = p.id } }
        }
        MLabel("LENGTH")
        HfSegmented(listOf("25m", "45m", "60m", "90m", "Custom"),
            if (custom) 4 else listOf(25, 45, 60, 90).indexOf(minutes).let { if (it < 0) 4 else it },
            onSelect = { i -> if (i == 4) custom = true else { custom = false; minutes = listOf(25, 45, 60, 90)[i] } })
        if (custom) {
            HfField(customText, {
                customText = it.filter(Char::isDigit).take(3)
                customText.toIntOrNull()?.let { v -> if (v in 5..480) minutes = v }
            }, "Minutes (5-480)", keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), mono = true)
            Text("Between 5 and 480 minutes.", fontSize = 12.sp, color = LocalHf.current.muted)
        }
        HfField(intent, { intent = it }, "Intent (optional)")
        Spacer(Modifier.height(2.dp))
        HfPrimary("Begin", onClick = { vm.startSession(placeId, minutes, intent); onClose() })
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
