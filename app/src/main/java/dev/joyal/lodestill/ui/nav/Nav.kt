package dev.joyal.lodestill.ui.nav

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.NavType
import dev.joyal.lodestill.AppViewModel
import dev.joyal.lodestill.ui.notes.NotesScreen
import dev.joyal.lodestill.ui.places.PlaceDetailScreen
import dev.joyal.lodestill.ui.places.PlacesScreen
import dev.joyal.lodestill.ui.places.UnfinishedScreen
import dev.joyal.lodestill.ui.settings.DistractionsScreen
import dev.joyal.lodestill.ui.settings.PermissionsScreen
import dev.joyal.lodestill.ui.settings.SettingsScreen
import dev.joyal.lodestill.ui.today.TodayScreen

@Composable
fun HfNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val tabs = listOf(
        Triple("today", "Today", Icons.Outlined.Schedule),
        Triple("places", "Places", Icons.Outlined.Apps),
        Triple("notes", "Notes", Icons.Outlined.Description),
        Triple("settings", "Settings", Icons.Outlined.Settings)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    Scaffold(bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            tabs.forEach { (r, label, icon) ->
                NavigationBarItem(
                    selected = route == r,
                    onClick = { nav.navigate(r) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                    icon = { Icon(icon, label) }, label = { Text(label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)))
            }
        }
    }) { pad ->
        NavHost(nav, "today", Modifier.padding(pad)) {
            composable("today") { TodayScreen(vm, nav) }
            composable("places") { PlacesScreen(vm, nav) }
            composable("notes") { NotesScreen(vm) }
            composable("settings") { SettingsScreen(vm, nav) }
            composable("permissions") { PermissionsScreen(vm, navDistractions = { nav.navigate("distractions") }) }
            composable("distractions") { DistractionsScreen(vm, nav) }
            composable("unfinished") { UnfinishedScreen(vm, nav) }
            composable("place/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                PlaceDetailScreen(vm, nav, it.arguments?.getLong("id") ?: 0)
            }
        }
    }
}
