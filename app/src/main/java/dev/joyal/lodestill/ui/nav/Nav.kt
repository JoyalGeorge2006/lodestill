package dev.joyal.lodestill.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.ui.notes.NotesScreen
import dev.joyal.lodestill.ui.places.PlaceDetailScreen
import dev.joyal.lodestill.ui.places.PlacesScreen
import dev.joyal.lodestill.ui.places.UnfinishedScreen
import dev.joyal.lodestill.ui.settings.DistractionsScreen
import dev.joyal.lodestill.ui.settings.PermissionsScreen
import dev.joyal.lodestill.ui.settings.SettingsScreen
import dev.joyal.lodestill.ui.theme.GeistMono
import dev.joyal.lodestill.ui.theme.LocalHf
import dev.joyal.lodestill.ui.today.TodayScreen

@Composable
fun HfNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val tabs = listOf("today" to "TODAY", "places" to "PLACES", "notes" to "NOTES", "settings" to "SETTINGS")
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val timerOnly by vm.timerOnly.collectAsState()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
            NavHost(nav, "today", Modifier.fillMaxSize()) {
                composable("today") { TodayScreen(vm, nav) }
                composable("places") { PlacesScreen(vm, nav) }
                composable("notes") { NotesScreen(vm) }
                composable("settings") { SettingsScreen(vm, nav) }
                composable("permissions") { PermissionsScreen(vm, nav) }
                composable("distractions") { DistractionsScreen(vm, nav) }
                composable("unfinished") { UnfinishedScreen(vm, nav) }
                composable("place/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    PlaceDetailScreen(vm, nav, it.arguments?.getLong("id") ?: 0)
                }
            }
        }
        if (!timerOnly) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(LocalHf.current.hair))
            Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                tabs.forEach { (r, label) ->
                    val on = route == r
                    Box(Modifier.clickable {
                        nav.navigate(r) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true }
                    }.background(if (on) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(label, fontFamily = GeistMono, fontSize = 10.sp, letterSpacing = 0.7.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (on) MaterialTheme.colorScheme.onPrimary else LocalHf.current.muted)
                    }
                }
            }
        }
    }
}
