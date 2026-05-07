package com.example.myfirstapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myfirstapp.data.GuiWuItem
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    items: List<GuiWuItem>,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (GuiWuItem) -> Unit,
    onDelete: (GuiWuItem) -> Unit
) {
    var itemToManage by remember { mutableStateOf<GuiWuItem?>(null) }
    var sortType by remember { mutableStateOf(SortType.DEFAULT) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Filled.Add, contentDescription = "添加物品")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("我的归物本", style = MaterialTheme.typography.headlineLarge)

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "排序菜单")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = type.title,
                                        color = if (sortType == type) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    sortType = type
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (items.isNotEmpty()) {
                val totalAssets = items.sumOf { it.price }
                val totalDailyCost = items.sumOf { item ->
                    val daysOwned = ChronoUnit.DAYS.between(item.date, LocalDate.now())
                    if (daysOwned > 0) item.price / daysOwned else item.price
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("当前总资产", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "¥ ${String.format(Locale.getDefault(), "%.2f", totalAssets)}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "整体日均成本：${String.format(Locale.getDefault(), "%.2f", totalDailyCost)} 元/天",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val sortedItems = remember(items, sortType) {
                when (sortType) {
                    SortType.DEFAULT -> items
                    SortType.PRICE_ASC -> items.sortedBy { it.price }
                    SortType.PRICE_DESC -> items.sortedByDescending { it.price }
                    SortType.TIME_ASC -> items.sortedByDescending { it.date }
                    SortType.TIME_DESC -> items.sortedBy { it.date }
                }
            }

            if (items.isEmpty()) {
                Text("还没有记录任何物品哦，点击右下角加号添加吧！", color = MaterialTheme.colorScheme.secondary)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sortedItems) { item ->
                        val daysOwned = ChronoUnit.DAYS.between(item.date, LocalDate.now())
                        val dailyCost = if (daysOwned > 0) item.price / daysOwned else item.price

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = { itemToManage = item }
                                ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(item.name, style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("购买日期: ${item.date}")
                                Text("已陪伴: $daysOwned 天")
                                Text(
                                    "购入价格: ${item.price} 元 (日均: ${
                                        String.format(Locale.getDefault(), "%.2f", dailyCost)
                                    } 元/天)"
                                )
                            }
                        }
                    }
                }
            }
        }

        itemToManage?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToManage = null },
                title = { Text("管理物品") },
                text = { Text("你想对【${item.name}】执行什么操作？") },
                confirmButton = {
                    TextButton(onClick = {
                        onNavigateToEdit(item)
                        itemToManage = null
                    }) { Text("修改") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        onDelete(item)
                        itemToManage = null
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
            )
        }
    }
}
