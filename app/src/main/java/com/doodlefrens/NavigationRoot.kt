package com.doodlefrens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.doodlefrens.ui.drawing.DrawingScreen
import com.doodlefrens.ui.setup.CreateRoomScreen
import com.doodlefrens.ui.setup.SelectRoomScreen
import com.doodlefrens.ui.setup.UsernameScreen

@Composable
fun NavigationRoot(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "setup"
    ) {
        setupGraph(navController)
        composable(route = "drawing/{username}/{roomName}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val roomName = backStackEntry.arguments?.getString("roomName") ?: ""
            DrawingScreen(
                username = username,
                roomName = roomName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

private fun NavGraphBuilder.setupGraph(navController: NavController) {
    navigation(
        startDestination = "username",
        route = "setup"
    ) {
        composable(route = "username") {
            UsernameScreen(
                onNavigateToSelectRoom = { username ->
                    navController.navigate("select_room/$username")
                }
            )
        }
        composable(route = "select_room/{username}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            SelectRoomScreen(
                username = username,
                onNewRoomClick = {
                    navController.navigate("create_room/$username")
                },
                onRoomSelected = { roomName ->
                    navController.navigate("drawing/$username/$roomName")
                }
            )
        }
        composable(route = "create_room/{username}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            CreateRoomScreen(
                username = username,
                onRoomCreated = { roomName ->
                    navController.navigate("drawing/$username/$roomName")
                }
            )
        }
    }
}
