package dev.joyal.lodestill.ui.places

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.ui.components.*

@Composable
fun PlacesScreen(vm: AppViewModel, nav: NavController) {
    val places by vm.places.collectAsState()
    val sessions by vm.allSessions.collectAsState()
    val scaffolding by vm.scaffolding.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Places", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
            TextButton(onClick = { showAdd = true }) { Text("+ Add") }
        }
        if (scaffolding.isNotEmpty()) {
            HfCard(onClick = { nav.navigate("unfinished") }) {
                HfRow(title = "Scaffolding", sub = "${scaffolding.size} interrupted sessions waiting to be finished")
            }
        }
        places.forEach { p ->
            val layers = vm.layersBuilt(p.id, sessions)
            val totalSec = vm.totalFocusSec(p.id, sessions)
            HfCard(onClick = { nav.navigate("place/${p.id}") }) {
                HfRow(title = p.name, sub = "$layers layers · total ${fmtClock(totalSec)}", end = { LayerStack(layers) })
            }
        }
    }
    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAdd = false }, confirmButton = {}, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("New place", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                HfPrimary("Add place", onClick = { if (name.isNotBlank()) { vm.addPlace(name, "book"); showAdd = false } })
                TextButton(onClick = { showAdd = false }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        })
    }
}

@Composable
fun PlaceDetailScreen(vm: AppViewModel, nav: NavController, placeId: Long) {
    val places by vm.places.collectAsState()
    val sessions by vm.allSessions.collectAsState()
    val place = places.firstOrNull { it.id == placeId } ?: return
    val layers = vm.layersBuilt(placeId, sessions)
    val total = vm.totalFocusSec(placeId, sessions)
    val days = vm.distinctDays(placeId, sessions)
    val placeSessions = sessions.filter { it.placeId == placeId }.sortedByDescending { it.startedAt }.take(10)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = { nav.popBackStack() }) { Text("‹ Back") }
        Text(place.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        HfCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("$layers", "layers")
                Stat(fmtClock(total), "total focus")
                Stat("$days", "days")
            }
        }
        HfCard {
            Text("Layers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LayerStack(layers)
            Spacer(Modifier.height(4.dp))
            Text("Every unbroken session adds one. Nothing decays.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HfCard {
            Text("Recent sessions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            placeSessions.forEach { s ->
                val label = when (s.layerState) {
                    "BUILT" -> "Layer built"; "SCAFFOLDING" -> "Scaffolding"; else -> "Resting"
                }
                HfRow(title = fmtClock(s.accumulatedSec), sub = AppViewModel.dayKey(s.startedAt) + " · " + label)
            }
            if (placeSessions.isEmpty()) Text("No sessions yet.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Stat(v: String, l: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(v, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(l, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun UnfinishedScreen(vm: AppViewModel, nav: NavController) {
    val scaffolding by vm.scaffolding.collectAsState()
    val places by vm.places.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { nav.popBackStack() }) { Text("‹ Back") }
        Text("Scaffolding", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Interrupted sessions hold their progress. Finish one to raise its layer.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        scaffolding.forEach { s ->
            val place = places.firstOrNull { it.id == s.placeId }
            HfCard {
                HfRow(title = place?.name ?: "Place",
                    sub = "${fmtClock(s.accumulatedSec)} of ${fmtClock(s.targetSec)} · started ${AppViewModel.dayKey(s.startedAt)}")
                Spacer(Modifier.height(8.dp))
                HfPrimary("Finish this layer", onClick = {
                    vm.startSession(s.placeId, 45, null, finishOf = s)
                    nav.navigate("today")
                })
            }
        }
        if (scaffolding.isEmpty()) Text("Nothing unfinished. Every layer is standing.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
