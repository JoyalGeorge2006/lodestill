package dev.joyal.lodestill.ui.places

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.ui.components.*
import dev.joyal.lodestill.ui.theme.LocalHf
import dev.joyal.lodestill.ui.today.HfDialog
import dev.joyal.lodestill.ui.today.HfField

@Composable
private fun Back(nav: NavController) {
    TextButton(onClick = { nav.popBackStack() }, contentPadding = PaddingValues(0.dp)) {
        Text("\u2039 Back", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
fun PlacesScreen(vm: AppViewModel, nav: NavController) {
    val places by vm.places.collectAsState()
    val sessions by vm.allSessions.collectAsState()
    val scaffolding by vm.scaffolding.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow("World")
        Text("Places", style = MaterialTheme.typography.headlineLarge)
        Text("Every unbroken session adds one layer. Nothing decays.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (scaffolding.isNotEmpty()) {
            HfCard(onClick = { nav.navigate("unfinished") }) {
                MLabel("SCAFFOLDING")
                HfRow(title = "${scaffolding.size} interrupted sessions waiting to be finished",
                    end = { MonoVal("\u203A") })
            }
        }
        places.forEach { p ->
            val layers = vm.layersBuilt(p.id, sessions)
            val totalSec = vm.totalFocusSec(p.id, sessions)
            val days = vm.distinctDays(p.id, sessions)
            HfCard(onClick = { nav.navigate("place/${p.id}") }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    MonoVal("\u203A")
                }
                LayerStack(layers, maxW = 400.dp, modifier = Modifier.fillMaxWidth())
                Text(if (layers == 0 && totalSec == 0L) "No sessions yet."
                    else "$layers layers \u00B7 $days days \u00B7 ${fmtDur(totalSec)}",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HfSecondary("+ Add place", onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth())
    }
    if (showAdd) {
        var name by remember { mutableStateOf("") }
        HfDialog(onDismiss = { showAdd = false }) {
            Text("New place", style = MaterialTheme.typography.titleMedium)
            Text("Every unbroken session adds one layer. Nothing decays.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HfField(name, { name = it }, "Name \u2014 e.g. Reading")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HfSecondary("Cancel", onClick = { showAdd = false }, modifier = Modifier.weight(1f))
                HfPrimary("Add place", onClick = { if (name.isNotBlank()) { vm.addPlace(name, "book"); showAdd = false } },
                    modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PlaceDetailScreen(vm: AppViewModel, nav: NavController, placeId: Long) {
    val places by vm.places.collectAsState()
    val sessions by vm.allSessions.collectAsState()
    val scaffolding by vm.scaffolding.collectAsState()
    val place = places.firstOrNull { it.id == placeId } ?: return
    val layers = vm.layersBuilt(placeId, sessions)
    val total = vm.totalFocusSec(placeId, sessions)
    val days = vm.distinctDays(placeId, sessions)
    val scaf = scaffolding.filter { it.placeId == placeId }
    val placeSessions = sessions.filter { it.placeId == placeId }.sortedByDescending { it.startedAt }.take(10)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Back(nav)
        Eyebrow("Place")
        Text(place.name, style = MaterialTheme.typography.headlineLarge)
        HfCard {
            LayerStack(layers, scaffolding = scaf.size, maxW = 400.dp, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom) {
                Text("$layers layers \u00B7 $days days", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Eyebrow("total ${fmtDur(total)}", color = LocalHf.current.muted)
            }
        }
        if (scaf.isNotEmpty()) HfCard {
            MLabel("SCAFFOLDING")
            scaf.forEach { s ->
                HfRow(title = "${fmtClock(s.accumulatedSec)} of ${fmtClock(s.targetSec)}",
                    sub = "started ${AppViewModel.dayKey(s.startedAt)}",
                    end = { MonoVal("Finish") },
                    onClick = { vm.startSession(s.placeId, 45, null, finishOf = s); nav.navigate("today") })
            }
        }
        HfCard {
            MLabel("RECENT SESSIONS")
            placeSessions.forEach { s ->
                val built = s.layerState == "BUILT"
                HfRow(title = fmtClock(s.accumulatedSec), sub = AppViewModel.dayKey(s.startedAt),
                    end = {
                        if (built) MonoVal("\u2713 layer", color = LocalHf.current.ok)
                        else MonoVal(if (s.layerState == "SCAFFOLDING") "scaffolding" else "resting")
                    })
            }
            if (placeSessions.isEmpty()) Text("No sessions yet.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun UnfinishedScreen(vm: AppViewModel, nav: NavController) {
    val scaffolding by vm.scaffolding.collectAsState()
    val places by vm.places.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Back(nav)
        Eyebrow("Held progress")
        Text("Unfinished", style = MaterialTheme.typography.headlineLarge)
        Text("Interrupted sessions hold their progress. Finish one to raise its layer.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (scaffolding.isNotEmpty()) HfCard {
            MLabel("SCAFFOLDING")
            scaffolding.forEach { s ->
                val place = places.firstOrNull { it.id == s.placeId }
                HfRow(title = place?.name ?: "Place",
                    sub = "${fmtClock(s.accumulatedSec)} of ${fmtClock(s.targetSec)} \u00B7 started ${AppViewModel.dayKey(s.startedAt)}",
                    end = { MonoVal("Finish  \u203A") },
                    onClick = { vm.startSession(s.placeId, 45, null, finishOf = s); nav.navigate("today") })
            }
        } else Text("Nothing unfinished. Every layer is standing.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
