package com.jonicodes.notetaker.presentation.navigation

sealed class Screen(val route: String) {
    data object Recording : Screen("recording")
    data object History : Screen("history")
    data object SummaryResult : Screen("summary_result/{transcript}/{participants}") {
        fun createRoute(transcript: String, participants: String): String {
            return "summary_result/${transcript}/${participants}"
        }
    }
    data object SummaryDetail : Screen("summary_detail/{summaryId}") {
        fun createRoute(summaryId: Long): String {
            return "summary_detail/$summaryId"
        }
    }
    data object Unsupported : Screen("unsupported")
}
