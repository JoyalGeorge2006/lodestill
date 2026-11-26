package dev.joyal.lodestill.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.ui.components.*

@Composable
fun NotesScreen(vm: AppViewModel) {
    val notes by vm.notes.collectAsState()
    val places by vm.places.collectAsState()
    val current by vm.current.collectAsState()
    var text by remember { mutableStateOf("") }
    var placeId by remember { mutableStateOf(0L) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Notes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        HfCard {
            OutlinedTextField(text, { text = it }, label = { Text("Capture a thought") },
                modifier = Modifier.fillMaxWidth(), maxLines = 3)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HfChip("No place", placeId == 0L) { placeId = 0L }
                places.take(3).forEach { p -> HfChip(p.name, placeId == p.id) { placeId = p.id } }
            }
            Spacer(Modifier.height(8.dp))
            HfPrimary("Save note", onClick = {
                vm.addNote(text, placeId.takeIf { it != 0L }, current?.id)
                text = ""
            })
        }
        notes.forEach { n ->
            val place = places.firstOrNull { it.id == n.placeId }
            HfCard {
                Text(n.text, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text((place?.name?.plus(" · ") ?: "") + AppViewModel.dayKey(n.createdAt),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (notes.isEmpty()) Text("No notes yet. Jot things down mid-session so they stop pulling at you.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
