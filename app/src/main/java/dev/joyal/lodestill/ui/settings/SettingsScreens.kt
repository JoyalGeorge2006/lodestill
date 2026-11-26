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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.ui.components.*
import dev.joyal.lodestill.ui.theme.LocalHf

@Composable
private fun Back(nav: NavController) {
    TextButton(onClick = { nav.popBackStack() }, contentPadding = PaddingValues(0.dp)) {
        Text("\u2039 Back", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavController) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsState()
    val blocked by vm.blockedApps.collectAsState()
    var showDisclosure by remember { mutableStateOf(false) }
    // Permission truth for the chosen tier, refreshed every time we return
    // to this screen (e.g. back from an Android Settings page).
    var usageOk by remember { mutableStateOf(vm.hasUsageAccess()) }
    var overlayOk by remember { mutableStateOf(vm.hasOverlay()) }
    var a11yOk by remember { mutableStateOf(vm.isAccessibilityOn()) }
    LifecycleResumeEffect(Unit) {
        usageOk = vm.hasUsageAccess(); overlayOk = vm.hasOverlay(); a11yOk = vm.isAccessibilityOn()
        onPauseOrDispose { }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow("Preferences")
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        HfCard {
            MLabel("APPEARANCE")
            HfSegmented(listOf("System", "Light", "Dark"),
                listOf("system", "light", "dark").indexOf(settings.theme).coerceAtLeast(0), onSelect = { i ->
                vm.setTheme(listOf("system", "light", "dark")[i])
            })
        }
        HfCard {
            MLabel("SESSION")
            Text("Default length", fontSize = 14.sp)
            HfSegmented(listOf("25m", "45m", "60m", "90m"),
                listOf(25, 45, 60, 90).indexOf(settings.defaultLengthMin), onSelect = { vm.setLen(listOf(25, 45, 60, 90)[it]) })
            HfRow(title = "Grace period", sub = "Time to return before a layer rests", end = { MonoVal("${settings.graceSec}s") })
            HfDivider()
            HfRow(title = "Finish window", end = { MonoVal("${settings.finishWindowH}h") })
        }
        HfCard {
            MLabel("FOCUS PROTECTION")
            HfSegmented(listOf("Off", "DND", "Guard", "Block"), settings.tier.coerceIn(0, 3), onSelect = { i ->
                when (i) {
                    3 -> showDisclosure = true
                    2 -> { vm.setTier(2); nav.navigate("permissions") }
                    else -> vm.setTier(i)
                }
            })
            Text(when (settings.tier) {
                1 -> "Do Not Disturb silences interruptions during sessions."
                2 -> "Guard shows a return banner over the distracting apps you choose. Needs usage access and overlay permission."
                3 -> "Block returns you to your session when one of your chosen distracting apps opens. Needs the accessibility service."
                else -> "Sessions run in-app only."
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Under battery saver, blocks or alerts can be late or missed.",
                style = MaterialTheme.typography.bodySmall, color = LocalHf.current.muted)
            // User-requested: a clear prompt with a button that opens the exact
            // Android Settings page for whatever the chosen tier is missing.
            val missing = mutableListOf<Pair<String, () -> Unit>>()
            if (settings.tier == 2) {
                if (!usageOk) missing.add("Usage access" to { ctx.startActivity(vm.usageSettingsIntent()) })
                if (!overlayOk) missing.add("Display over other apps" to { ctx.startActivity(vm.overlaySettingsIntent()) })
            }
            if (settings.tier == 3 && !a11yOk)
                missing.add("Accessibility service" to { ctx.startActivity(vm.accessibilitySettingsIntent()) })
            missing.forEach { (label, open) ->
                HfDivider()
                HfRow(title = "Needs permission: $label",
                    sub = "Protection is off until this is granted",
                    end = { MonoVal("Open", color = MaterialTheme.colorScheme.error) },
                    onClick = open)
            }
            HfDivider()
            HfRow(title = "Do Not Disturb", sub = "Silence interruptions while a session runs", end = {
                Switch(checked = settings.dndEnabled, onCheckedChange = { vm.setDndEnabled(it) })
            })
            HfDivider()
            HfRow(title = "Blocking and permissions", end = { MonoVal("\u203A") }, onClick = { nav.navigate("permissions") })
            HfDivider()
            HfRow(title = "Distracting apps", end = { MonoVal("${blocked.size} selected  \u203A") },
                onClick = { nav.navigate("distractions") })
        }
        HfCard {
            MLabel("ABOUT")
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
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onCancel, contentPadding = PaddingValues(0.dp)) {
                        Text("\u2039 Back", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }
                    Eyebrow("Disclosure")
                    Text("Accessibility blocking", style = MaterialTheme.typography.titleLarge)
                    Text("While a focus session is active, Lodestill notices when one of your chosen distracting apps opens and returns you to your session.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HfCard {
                        MLabel("WHAT IT DOES")
                        Text("You pick the apps; only those are affected. It does not change other apps.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HfCard {
                        MLabel("WHAT IT NEVER DOES")
                        Text("It never reads screen content, messages, passwords or what you type. It only sees which app opened.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HfCard {
                        MLabel("LIMITS")
                        Text("Under battery saver, blocks or alerts can be late or missed.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HfPrimary("I understand, continue", onClick = onAccept)
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
fun PermissionsScreen(vm: AppViewModel, nav: NavController) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsState()
    var dndGranted by remember { mutableStateOf(vm.hasDndAccess()) }
    var usageGranted by remember { mutableStateOf(vm.hasUsageAccess()) }
    var overlayGranted by remember { mutableStateOf(vm.hasOverlay()) }
    var a11yOn by remember { mutableStateOf(vm.isAccessibilityOn()) }
    LifecycleResumeEffect(Unit) {
        dndGranted = vm.hasDndAccess(); usageGranted = vm.hasUsageAccess()
        overlayGranted = vm.hasOverlay(); a11yOn = vm.isAccessibilityOn()
        onPauseOrDispose { }
    }
    fun state(ok: Boolean, on: String = "Granted", off: String = "Not granted") = if (ok) on else off
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Back(nav)
        Eyebrow("Focus protection")
        Text("Blocking and permissions", style = MaterialTheme.typography.titleLarge)
        HfSegmented(listOf("Off", "DND", "Guard", "Block"), settings.tier.coerceIn(0, 3), onSelect = { i ->
            vm.setTier(i)
        })
        HfCard {
            HfRow(title = "Off", sub = "Sessions run in-app only.")
            HfDivider()
            HfRow(title = "DND", sub = "Silences interruptions during sessions.")
            HfDivider()
            HfRow(title = "Guard", sub = "Shows a return banner over the apps you choose. Needs usage access and overlay.")
            HfDivider()
            HfRow(title = "Block", sub = "Returns you to your session when a chosen app opens. Needs accessibility.")
        }
        HfCard {
            MLabel("PERMISSIONS")
            HfRow(title = "Notifications", sub = "timer alerts and reminders", end = { MonoVal("Open") }, onClick = {
                ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)) })
            HfDivider()
            HfRow(title = "Do Not Disturb access", sub = "silences interruptions during sessions",
                end = { if (dndGranted) MonoVal("\u2713", color = LocalHf.current.ok) else MonoVal("Open") },
                onClick = { ctx.startActivity(vm.dndSettingsIntent()) })
            HfDivider()
            HfRow(title = "Usage access", sub = "lets Guard see when another app opens",
                end = { if (usageGranted) MonoVal("\u2713", color = LocalHf.current.ok) else MonoVal("Open") },
                onClick = { ctx.startActivity(vm.usageSettingsIntent()) })
            HfDivider()
            HfRow(title = "Display over other apps", sub = "lets Guard show the return banner",
                end = { if (overlayGranted) MonoVal("\u2713", color = LocalHf.current.ok) else MonoVal("Open") },
                onClick = { ctx.startActivity(vm.overlaySettingsIntent()) })
            HfDivider()
            HfRow(title = "Accessibility service", sub = "lets Block return you to your session",
                end = { if (a11yOn) MonoVal("On", color = LocalHf.current.ok) else MonoVal("Off") },
                onClick = { ctx.startActivity(vm.accessibilitySettingsIntent()) })
            HfDivider()
            HfRow(title = "Distracting apps", sub = "Choose which apps Guard and Block act on",
                end = { MonoVal("\u203A") }, onClick = { nav.navigate("distractions") })
        }
        Text("Under battery saver, blocks or alerts can be late or missed. Lodestill only asks for what the timer needs, and nothing leaves your device.",
            style = MaterialTheme.typography.bodySmall, color = LocalHf.current.muted)
    }
}
