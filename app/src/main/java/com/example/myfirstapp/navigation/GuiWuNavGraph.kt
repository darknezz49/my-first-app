package com.example.myfirstapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myfirstapp.data.GuiWuDao
import com.example.myfirstapp.ui.AddEditScreen
import com.example.myfirstapp.ui.HomeScreen
import com.example.myfirstapp.ui.StatScreen
import kotlinx.coroutines.launch

@Composable
fun GuiWuBenApp(dao: GuiWuDao) {
    val items by remember { dao.getAllItems() }.collectAsState(initial = emptyList())
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = {
                        if (currentRoute != "home") {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "首页") },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = currentRoute == "stats",
                    onClick = {
                        if (currentRoute != "stats") {
                            navController.navigate("stats") {
                                popUpTo("home")
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Star, contentDescription = "统计") },
                    label = { Text("统计") }
                )
            }
        }
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(scaffoldPadding)
        ) {
            composable("home") {
                HomeScreen(
                    items = items,
                    onNavigateToAdd = { navController.navigate("add_item") },
                    onNavigateToEdit = { item -> navController.navigate("add_item/${item.id}") },
                    onDelete = { item ->
                        coroutineScope.launch { dao.deleteItem(item) }
                    }
                )
            }
            composable("stats") {
                StatScreen(items = items)
            }
            composable(
                route = "add_item/{itemId}",
                arguments = listOf(navArgument("itemId") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                val itemToEdit = items.find { it.id == itemId }
                AddEditScreen(
                    navController = navController,
                    itemToEdit = itemToEdit,
                    onSave = { newItem ->
                        coroutineScope.launch { dao.insertItem(newItem) }
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable("add_item") {
                AddEditScreen(
                    navController = navController,
                    itemToEdit = null,
                    onSave = { newItem ->
                        coroutineScope.launch { dao.insertItem(newItem) }
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}
