package com.z_company.route.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.z_company.domain.navigation.Router
import com.z_company.route.component.BottomNavigationBar
import com.z_company.route.navigation.MainNavigation

@Composable
fun MainScreen(router: Router) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = MaterialTheme.colorScheme.background // Set background color to avoid the white flashing when you switch between screens
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            MainNavigation(router = router, navController = navController)
        }
    }
}