package dev.joyal.lodestill.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.core.Engine
import dev.joyal.lodestill.data.Place
import dev.joyal.lodestill.ui.components.*

@Composable
fun TodayScreen(vm: AppViewModel, nav: NavController) {
    val current by vm.current.collectAsState()
    val now by vm.now.collectAsState()
    val places by vm.places.collectAsState()
    val sessions by vm.allSessions.collectAsState()
    val scaffolding by vm.scaffolding.collectAsState()
    val settings by vm.settings.collectAsState()
    var showStart by remember { mutableStateOf(false) }
    var showEndEarly by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Today", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        val s = current
        if (s == null) {
            // start card
            HfCard {
                Text("Ready to focus?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("${settings.defaultLengthMin} minutes builds one layer at any place.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                HfPrimary("Start a session", onClick = { showStart = true })
            }
            if (scaffolding.isNotEmpty()) {
                HfCard {
                    HfRow(title = "Scaffolding", sub = "${scaffolding.size} interrupted · finish them to raise a layer", end = {
                        TextButton(onClick = { nav.navigate("unfinished") }) { Text("View") }
                    })
                }
            }
        } else {
            val place = places.firstOrNull { it.id == s.placeId }
            when (s.status) {
                "ACTIVE" -> {
                    val remaining = Engine.remainingSec(s, now)
                    val progress = 1f - remaining.toFloat() / s.targetSec.toFloat()
                    HfCard {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(place?.name ?: "Focus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            s.intent?.let { Text(it, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Spacer(Modifier.height(16.dp))
                            TimerRing(progress.coerceIn(0f, 1f), Engine.fmtRemaining(remaining), "remaining")
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HfSecondary("Pause", onClick = { vm.pause() }, modifier = Modifier.weight(1f))
                                HfSecondary("End early", onClick = { showEndEarly = true }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Text("Leave the app? You have ${settings.graceSec} seconds to come back before the layer becomes scaffolding.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                "PAUSED" -> HfCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Paused", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        TimerRing((s.accumulatedSec.toFloat() / s.targetSec).coerceIn(0f, 1f),
                            Engine.fmtRemaining(Engine.remainingSec(s, now)), "remaining")
                        Spacer(Modifier.height(12.dp))
                        HfPrimary("Resume", onClick = { vm.unpause() })
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showEndEarly = true }) { Text("End early") }
                    }
                }
                "INTERRUPTED" -> HfCard {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Interrupted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("${fmtClock(s.accumulatedSec)} held as scaffolding. Pick up where you left off, any time within ${settings.finishWindowH} hours.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        HfPrimary("Continue session", onClick = { vm.resumeInterrupted() })
                        Spacer(Modifier.height(8.dp))
                        HfSecondary("Let it rest", onClick = { vm.endEarly() })
                    }
                }
                "COMPLETED" -> {
                    HfCard {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Layer built", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("${fmtClock(s.accumulatedSec)} at ${place?.name ?: "your place"}",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            HfPrimary("Done", onClick = { vm.endEarly() })
                        }
                    }
                }
            }
        }

        // today summary
        val todaySec = vm.todayFocusSec(sessions) + (current?.accumulatedSec ?: 0)
        HfCard {
            Text("Today's focus", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            places.forEach { p ->
                val layers = vm.layersBuilt(p.id, sessions)
                if (layers > 0) HfRow(title = p.name, sub = "$layers layers", end = { LayerStack(layers) })
            }
            if (places.none { vm.layersBuilt(it.id, sessions) > 0 })
                Text("No layers yet today. The first one is the easiest.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("Total today: ${fmtClock(todaySec)}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showStart) StartSheet(vm, places, settings.defaultLengthMin) { showStart = false }
    if (showEndEarly) {
        val kept = current?.accumulatedSec ?: 0
        AlertDialog(onDismissRequest = { showEndEarly = false }, confirmButton = {}, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("End early?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${fmtClock(kept)} is kept as scaffolding. Finish it within ${settings.finishWindowH} hours and the layer still counts.",
                    style = MaterialTheme.typography.bodyMedium)
                HfPrimary("Keep focusing", onClick = { showEndEarly = false })
                HfSecondary("End early", onClick = { vm.endEarly(); showEndEarly = false })
            }
        })
    }
}

@Composable
fun StartSheet(vm: AppViewModel, places: List<Place>, defaultMin: Int, onClose: () -> Unit) {
    var placeId by remember { mutableStateOf(places.firstOrNull()?.id ?: 0L) }
    var minutes by remember { mutableStateOf(defaultMin) }
    var custom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var intent by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, confirmButton = {}, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Start a session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Place", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                places.forEach { p ->
                    HfChip(p.name, p.id == placeId) { placeId = p.id }
                }
            }
            Text("Length", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 45, 60, 90).forEach { m -> HfChip("${m}m", minutes == m && !custom) { minutes = m; custom = false } }
                HfChip("Custom", custom) { custom = true }
            }
            if (custom) OutlinedTextField(value = customText, onValueChange = {
                customText = it.filter(Char::isDigit).take(3)
                customText.toIntOrNull()?.let { v -> if (v in 5..480) minutes = v }
            }, label = { Text("Minutes (5-480)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = intent, onValueChange = { intent = it },
                label = { Text("Intent (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            HfPrimary("Begin", onClick = { vm.startSession(placeId, minutes, intent); onClose() })
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    })
}
