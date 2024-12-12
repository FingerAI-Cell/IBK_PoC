// ui/navigation/NavGraph.kt
package com.ibkpoc.amn.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.ibkpoc.amn.ui.screens.main.MainScreen
import com.ibkpoc.amn.viewmodel.MainViewModel
import com.ibkpoc.amn.ui.navigation.Screen

// Screen 클래스는 별도 파일로 이동했다고 가정
fun NavGraphBuilder.appNavGraph(
    viewModel: MainViewModel,
    @Suppress("UNUSED_PARAMETER") navController: NavController  // 현재는 사용하지 않지만 향후 사용 예정
) {
    composable(route = Screen.Main.route) {
        val recordingState = viewModel.recordingState.collectAsState().value
        val errorMessage = viewModel.errorMessage.collectAsState().value
        val isLoading = viewModel.isLoading.collectAsState().value
        
        MainScreen(
            recordingState = recordingState,
            errorMessage = errorMessage,
            isLoading = isLoading,
            onStartMeeting = { viewModel.startMeeting() },
            onEndMeeting = { viewModel.endMeeting() }
        )
    }
}