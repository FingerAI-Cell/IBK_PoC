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
    navController: NavController
) {
    composable(route = Screen.Main.route) {
        MainScreen(viewModel = viewModel)
    }
}