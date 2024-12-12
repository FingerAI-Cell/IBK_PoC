// ui/navigation/Screen.kt
package com.ibkpoc.amn.ui.navigation

sealed class Screen {
    abstract val route: String

    object Main : Screen() {
        override val route: String = "main"
    }

    object Detail : Screen() {
        override val route: String = "detail/{id}"
        fun createRoute(id: String) = "detail/$id"
    }
}