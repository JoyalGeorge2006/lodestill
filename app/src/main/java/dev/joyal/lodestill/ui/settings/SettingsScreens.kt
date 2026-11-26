package dev.joyal.lodestill.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.ui.components.*

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavController) {
    val settings by vm.settings.collectAsState()
    var showDisclosure by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        HfCard {
            Text("Session", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Default length", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 45, 60, 90).forEach { m ->
                    HfChip("${m}m", settings.defaultLengthMin == m) { vm.setLen(m) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Grace period ${settings.graceSec}s · finish window ${settings.finishWindowH}h",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HfCard {
            Text("Focus protection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            HfSegmented(listOf("Off", "DND", "Guard", "Block"), settings.tier.coerceIn(0, 3), onSelect = { i ->
                when (i) {
                    3 -> showDisclosure = true
                    2 -> { vm.setTier(2); nav.navigate("permissions") }
                    else -> vm.setTier(i)
                }
            })
            Spacer(Modifier.height(8.dp))
            Text(when (settings.tier) {
                1 -> "Do Not Disturb silences interruptions during sessions."
                2 -> "Guard shows a return banner over the distracting apps you choose. Needs usage access and overlay permission."
                3 -> "Block returns you to your session when one of your chosen distracting apps opens. Needs the accessibility service."
                else -> "Sessions run in-app only."
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("Under battery saver, blocks or alerts can be late or missed.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (settings.tier == 3) {
                var a11yOn by remember { mutableStateOf(vm.isAccessibilityOn()) }
                LaunchedEffect(Unit) {
                    while (true) { a11yOn = vm.isAccessibilityOn(); kotlinx.coroutines.delay(2_000) }
                }
                if (!a11yOn) {
                    Spacer(Modifier.height(6.dp))
                    Text("Block is off: the accessibility service is disabled. This can happen after force-stopping Lodestill.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { nav.navigate("permissions") }) { Text("Fix in Blocking and permissions") }
                }
            }
        }
        HfCard {
            Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            HfSegmented(listOf("System", "Light", "Dark"),
                listOf("system", "light", "dark").indexOf(settings.theme).coerceAtLeast(0), onSelect = { i ->
                vm.setTheme(listOf("system", "light", "dark")[i])
            })
        }
        HfCard(onClick = { nav.navigate("distractions") }) {
            HfRow(title = "Distracting apps", sub = "Guard and Block act only on the apps you pick")
        }
        HfCard {
            HfRow(title = "Do Not Disturb", sub = "Silence interruptions while a session runs", end = {
                Switch(checked = settings.dndEnabled, onCheckedChange = { vm.setDndEnabled(it) })
            })
        }
        HfCard(onClick = { nav.navigate("permissions") }) {
            HfRow(title = "Blocking and permissions", sub = "Notifications, DND, usage access, overlay, accessibility")
        }
        HfCard {
            Text("About", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Lodestill builds places out of unbroken focus. An interrupted session leaves scaffolding, never a broken streak. Progress never decays.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (showDisclosure) AccessibilityDisclosure(
        onAccept = { vm.setTier(3); showDisclosure = false; nav.navigate("permissions") },
        onCancel = { showDisclosure = false })
}

@Composable
fun AccessibilityDisclosure(onAccept: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel, confirmButton = {}, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Accessibility blocking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("While a focus session is active, Lodestill notices when one of your chosen distracting apps opens and returns you to your session. You pick the apps; only those are affected. Emergency, calling and system navigation always stay reachable.",
                style = MaterialTheme.typography.bodyMedium)
            Text("It never reads screen content, messages, passwords or what you type. It only sees which app opened.",
                style = MaterialTheme.typography.bodyMedium)
            Text("Under battery saver, blocks or alerts can be late or missed. Force-stopping Lodestill can turn Block off; re-enable it from Blocking and permissions.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HfPrimary("I understand, continue", onClick = onAccept)
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Not now") }
        }
    })
}

@Composable
fun PermissionsScreen(vm: AppViewModel, navDistractions: () -> Unit) {
    val ctx = LocalContext.current
    var dndGranted by remember { mutableStateOf(vm.hasDndAccess()) }
    var usageGranted by remember { mutableStateOf(vm.hasUsageAccess()) }
    var overlayGranted by remember { mutableStateOf(vm.hasOverlay()) }
    var a11yOn by remember { mutableStateOf(vm.isAccessibilityOn()) }
    LaunchedEffect(Unit) {
        dndGranted = vm.hasDndAccess(); usageGranted = vm.hasUsageAccess()
        overlayGranted = vm.hasOverlay(); a11yOn = vm.isAccessibilityOn()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Blocking and permissions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        HfCard {
            HfRow(title = "Notifications", sub = "Timer alerts and session reminders", end = {
                TextButton(onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }) { Text("Open") }
            })
        }
        HfCard(onClick = navDistractions) {
            HfRow(title = "Distracting apps", sub = "Guard and Block act only on the apps you pick")
        }
        HfCard {
            HfRow(title = "Do Not Disturb",
                sub = (if (dndGranted) "Granted · " else "Not granted · ") + "silences interruptions during sessions", end = {
                TextButton(onClick = { ctx.startActivity(vm.dndSettingsIntent()) }) { Text("Open") }
            })
        }
        HfCard {
            HfRow(title = "Usage access",
                sub = (if (usageGranted) "Granted · " else "Not granted · ") + "lets Guard see when another app opens", end = {
                TextButton(onClick = { ctx.startActivity(vm.usageSettingsIntent()) }) { Text("Open") }
            })
        }
        HfCard {
            HfRow(title = "Display over other apps",
                sub = (if (overlayGranted) "Granted · " else "Not granted · ") + "lets Guard show the return banner", end = {
                TextButton(onClick = { ctx.startActivity(vm.overlaySettingsIntent()) }) { Text("Open") }
            })
        }
        HfCard {
            HfRow(title = "Accessibility service",
                sub = (if (a11yOn) "On · " else "Off · ") + "lets Block return you to your session", end = {
                TextButton(onClick = { ctx.startActivity(vm.accessibilitySettingsIntent()) }) { Text("Open") }
            })
        }
        HfCard(onClick = { navDistractions() }) {
            HfRow(title = "Distracting apps", sub = "Choose which apps Guard and Block act on")
        }
        Text("Under battery saver, blocks or alerts can be late or missed. Lodestill only asks for what the timer needs, and nothing leaves your device.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
