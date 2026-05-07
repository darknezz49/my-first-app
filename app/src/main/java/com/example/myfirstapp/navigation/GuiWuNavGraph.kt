package com.example.myfirstapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myfirstapp.data.GuiWuDao
import com.example.myfirstapp.ui.AddEditScreen
import com.example.myfirstapp.ui.HomeScreen
import kotlinx.coroutines.launch

@Composable
fun GuiWuBenApp(dao: GuiWuDao) {
    val items by remember { dao.getAllItems() }.collectAsState(initial = emptyList())
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = "home") {
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
