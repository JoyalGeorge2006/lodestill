package dev.joyal.lodestill.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.ui.components.*
import dev.joyal.lodestill.ui.theme.LocalHf
import dev.joyal.lodestill.ui.today.HfField

@Composable
fun DistractionsScreen(vm: AppViewModel, nav: NavController) {
    val blocked by vm.blockedApps.collectAsState()
    val settings by vm.settings.collectAsState()
    var query by remember { mutableStateOf("") }
    val apps = remember { vm.installedLaunchableApps() }
    val shown = apps.filter { query.isBlank() || it.second.contains(query, true) }
    var usage by remember { mutableStateOf(vm.hasUsageAccess()) }
    var overlay by remember { mutableStateOf(vm.hasOverlay()) }
    var a11y by remember { mutableStateOf(vm.isAccessibilityOn()) }
    LifecycleResumeEffect(Unit) {
        usage = vm.hasUsageAccess(); overlay = vm.hasOverlay(); a11y = vm.isAccessibilityOn()
        onPauseOrDispose { }
    }
    // "Protection is on" only when the chosen tier's required permissions are actually granted.
    val active = when (settings.tier) { 2 -> usage && overlay; 3 -> a11y; else -> false }
    val needsPerm = settings.tier >= 2 && !active
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = { nav.popBackStack() }, contentPadding = PaddingValues(0.dp)) {
            Text("\u2039 Back", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }
        Eyebrow("Guard and Block act only on the apps you pick")
        Text("Distracting apps", style = MaterialTheme.typography.titleLarge)
        HfField(query, { query = it }, "Search apps")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${blocked.size} selected", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when {
                active -> Eyebrow("protection is on", color = LocalHf.current.ok)
                needsPerm -> Eyebrow("permission needed", color = MaterialTheme.colorScheme.error)
                else -> {}
            }
        }
        if (blocked.isEmpty() && active)
            Text("No apps selected yet. Protection is on but no apps are chosen.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        if (needsPerm) TextButton(onClick = { nav.navigate("permissions") }, contentPadding = PaddingValues(0.dp)) {
            Text("Open blocking and permissions", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface) }
        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.first }) { (pkg, label) ->
                val on = blocked.any { it.packageName == pkg }
                HfRow(title = label, sub = pkg, end = {
                    Checkbox(checked = on, onCheckedChange = { vm.toggleBlocked(pkg, label) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                            uncheckedColor = LocalHf.current.hair2))
                }, onClick = { vm.toggleBlocked(pkg, label) })
                HfDivider()
            }
        }
        Text("Only the apps you pick are affected.", fontSize = 12.sp, color = LocalHf.current.muted)
    }
}
