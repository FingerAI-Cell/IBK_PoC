// ui/navigation/Screen.kt
package com.ibkpoc.amn.ui.navigation

sealed class Screen {
    abstract val route: String
    
    data class Main(
        override val route: String = "main"
    ) : Screen() {
        companion object {
            const val route = "main"
        }
    }
    
    data class Meeting(
        val meetingId: Long = 0L,
        val userId: Int = 0,
        override val route: String = "meeting/$meetingId/$userId"
    ) : Screen() {
        companion object {
            const val baseRoute = "meeting/{meetingId}/{userId}"
            fun createRoute(meetingId: Long, userId: Int) = "meeting/$meetingId/$userId"
        }
    }
    
    data class List(
        override val route: String = "list"
    ) : Screen() {
        companion object {
            const val route = "list"
        }
    }
    
    data class Detail(
        val meetingId: Long,
        val title: String,
        val start: String,
        val end: String,
        val participant: Int,
        val topic: String?,
        override val route: String = createRoute(
            meetingId, title, start, end, participant, topic
        )
    ) : Screen() {
        companion object {
            const val baseRoute = 
                "detail/{meetingId}/{title}/{start}/{end}/{participant}/{topic}"
            
            fun createRoute(
                meetingId: Long,
                title: String,
                start: String,
                end: String,
                participant: Int,
                topic: String?
            ) = "detail/$meetingId/$title/$start/$end/$participant/${topic ?: "no-topic"}"
        }
    }
}