package dev.joyal.lodestill.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.ui.components.*

@Composable
fun DistractionsScreen(vm: AppViewModel, nav: NavController) {
    val blocked by vm.blockedApps.collectAsState()
    val settings by vm.settings.collectAsState()
    var query by remember { mutableStateOf("") }
    val apps = remember { vm.installedLaunchableApps() }
    val shown = apps.filter { query.isBlank() || it.second.contains(query, true) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { nav.popBackStack() }) { Text("‹ Back") }
        Text("Distracting apps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Only the apps you pick here are affected. Emergency, calling and system navigation always stay reachable.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(query, { query = it }, label = { Text("Search apps") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (blocked.isEmpty() && settings.tier >= 2)
            Text("No apps selected yet - protection is on but nothing is blocked.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Text("${blocked.size} selected" + if (settings.tier >= 2) " · protection is on" else "",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(shown, key = { it.first }) { (pkg, label) ->
                val on = blocked.any { it.packageName == pkg }
                HfGroup {
                    HfRow(title = label, sub = pkg, end = {
                        Switch(checked = on, onCheckedChange = { vm.toggleBlocked(pkg, label) })
                    }, onClick = { vm.toggleBlocked(pkg, label) })
                }
            }
        }
    }
}
