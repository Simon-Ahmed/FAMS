package com.fams.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fams.app.domain.model.UserRole
import com.fams.app.ui.screens.auth.LoginScreen
import com.fams.app.ui.screens.coordinator.CoordinatorDashboard
import com.fams.app.ui.screens.teacher.TeacherDashboard
import com.fams.app.ui.screens.student.StudentDashboard
import com.fams.app.ui.screens.classrep.ClassRepDashboard

@Composable
fun FAMSNavGraph(
    navController: NavHostController,
    startDestination: String,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(NavRoutes.LOGIN) {
            LoginScreen(onLoginSuccess = { role ->
                val dest = when (role) {
                    UserRole.COORDINATOR -> NavRoutes.COORDINATOR_DASHBOARD
                    UserRole.TEACHER -> NavRoutes.TEACHER_DASHBOARD
                    UserRole.STUDENT -> NavRoutes.STUDENT_DASHBOARD
                    UserRole.CLASS_REP -> NavRoutes.CLASS_REP_DASHBOARD
                }
                navController.navigate(dest) { popUpTo(NavRoutes.LOGIN) { inclusive = true } }
            })
        }

        composable(NavRoutes.COORDINATOR_DASHBOARD) {
            CoordinatorDashboard(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle,
                onSignOut = { navController.navigate(NavRoutes.LOGIN) { popUpTo(0) { inclusive = true } } })
        }

        composable(NavRoutes.TEACHER_DASHBOARD) {
            TeacherDashboard(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle,
                onSignOut = { navController.navigate(NavRoutes.LOGIN) { popUpTo(0) { inclusive = true } } })
        }

        composable(NavRoutes.STUDENT_DASHBOARD) {
            StudentDashboard(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle,
                onSignOut = { navController.navigate(NavRoutes.LOGIN) { popUpTo(0) { inclusive = true } } })
        }

        composable(NavRoutes.CLASS_REP_DASHBOARD) {
            ClassRepDashboard(isDarkTheme = isDarkTheme, onThemeToggle = onThemeToggle,
                onSignOut = { navController.navigate(NavRoutes.LOGIN) { popUpTo(0) { inclusive = true } } })
        }
    }
}
