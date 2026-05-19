package com.fams.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.fams.app.di.FirebaseModule
import com.fams.app.ui.navigation.FAMSNavGraph
import com.fams.app.ui.navigation.NavRoutes
import com.fams.app.ui.theme.FAMSTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            FAMSTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                FAMSNavGraph(
                    navController = navController,
                    startDestination = NavRoutes.LOGIN,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}
