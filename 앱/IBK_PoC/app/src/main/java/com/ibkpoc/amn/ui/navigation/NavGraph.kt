// ui/navigation/NavGraph.kt
package com.ibkpoc.amn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.composable
import com.ibkpoc.amn.ui.screens.main.MainScreen
import com.ibkpoc.amn.ui.screens.meeting.MeetingScreen
import com.ibkpoc.amn.ui.screens.list.MeetingListScreen
import com.ibkpoc.amn.ui.screens.detail.MeetingDetailScreen
import com.ibkpoc.amn.viewmodel.MainViewModel
import com.ibkpoc.amn.model.MeetingListResponse
import com.ibkpoc.amn.viewmodel.MeetingListViewModel

fun NavGraphBuilder.appNavGraph(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    composable(Screen.Main().route) {
        MainScreen(viewModel, onNavigate = { screen ->
            handleNavigation(navController, screen)
        })
    }

    composable(Screen.List().route) {
        val meetingListViewModel = hiltViewModel<MeetingListViewModel>()
        MeetingListScreen(
            viewModel = meetingListViewModel,
            onNavigate = { screen ->
                handleNavigation(navController, screen)
            }
        )
    }

    composable(
        route = Screen.Meeting.baseRoute,
        arguments = listOf(
            navArgument("meetingId") { type = NavType.LongType },
            navArgument("userId") { type = NavType.IntType }
        )
    ) { backStackEntry ->
        val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: 0L
        val userId = backStackEntry.arguments?.getInt("userId") ?: 0

        MeetingScreen(
            meetingId = meetingId,
            userId = userId,
            viewModel = viewModel,
            onNavigate = { screen ->
                handleNavigation(navController, screen)
            }
        )
    }

    composable(
        route = Screen.Detail.baseRoute,
        arguments = listOf(
            navArgument("meetingId") { type = NavType.LongType },
            navArgument("title") { type = NavType.StringType },
            navArgument("start") { type = NavType.StringType },
            navArgument("end") { type = NavType.StringType },
            navArgument("participant") { type = NavType.IntType },
            navArgument("topic") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: 0L
        val title = backStackEntry.arguments?.getString("title") ?: ""
        val start = backStackEntry.arguments?.getString("start") ?: ""
        val end = backStackEntry.arguments?.getString("end") ?: ""
        val participant = backStackEntry.arguments?.getInt("participant") ?: 0
        val topic = backStackEntry.arguments?.getString("topic") ?: ""
        
        MeetingDetailScreen(
            meeting = MeetingListResponse(
                id = meetingId,
                title = title,
                start = start,
                end = end,
                participant = participant,
                topic = topic
            ),
            onNavigate = { screen ->
                navController.navigate(screen.route)
            }
        )
    }
}

// 공통 네비게이션 로직 처리 함수
private fun handleNavigation(navController: NavHostController, screen: Screen) {
    when (screen) {
        is Screen.Main -> {
            navController.popBackStack()
            navController.navigate(screen.route) {
                popUpTo(Screen.Main().route) { inclusive = true }
            }
        }
        is Screen.Detail -> {
            navController.navigate(screen.route)
        }
        else -> {
            navController.navigate(screen.route) {
                launchSingleTop = true
            }
        }
    }
}