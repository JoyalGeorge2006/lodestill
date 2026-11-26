package dev.joyal.lodestill.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.ui.components.*
import dev.joyal.lodestill.ui.theme.LocalHf
import dev.joyal.lodestill.ui.today.HfField

@Composable
fun NotesScreen(vm: AppViewModel) {
    val notes by vm.notes.collectAsState()
    val places by vm.places.collectAsState()
    val current by vm.current.collectAsState()
    var text by remember { mutableStateOf("") }
    var placeId by remember { mutableStateOf(0L) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow("Mid-session capture")
        Text("Notes", style = MaterialTheme.typography.headlineLarge)
        HfCard {
            HfField(text, { text = it }, "Capture a thought", maxLines = 3)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HfChip("No place", placeId == 0L) { placeId = 0L }
                places.take(3).forEach { p -> HfChip(p.name, placeId == p.id) { placeId = p.id } }
            }
            HfSecondary("Save note", onClick = {
                vm.addNote(text, placeId.takeIf { it != 0L }, current?.id)
                text = ""
            }, modifier = Modifier.fillMaxWidth())
        }
        if (notes.isNotEmpty()) HfCard {
            notes.forEach { n ->
                val place = places.firstOrNull { it.id == n.placeId }
                HfRow(title = n.text, sub = (place?.name ?: "No place") + " \u00B7 " + AppViewModel.dayKey(n.createdAt))
            }
        } else Text("No notes yet. Jot things down mid-session so they stop pulling at you.",
            style = MaterialTheme.typography.bodyMedium, color = LocalHf.current.muted)
    }
}
