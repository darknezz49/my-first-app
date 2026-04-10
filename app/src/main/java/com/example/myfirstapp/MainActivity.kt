package com.example.myfirstapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

// ==========================================
// 1. 数据库配置区 (Room Database)
// ==========================================

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? = date?.toString()
}

@Entity(tableName = "guiwu_items")
data class GuiWuItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val date: LocalDate
)

@Dao
interface GuiWuDao {
    @Query("SELECT * FROM guiwu_items ORDER BY date DESC")
    fun getAllItems(): Flow<List<GuiWuItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GuiWuItem)

    @Delete
    suspend fun deleteItem(item: GuiWuItem)
}

@Database(entities = [GuiWuItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun guiwuDao(): GuiWuDao
}

// ==========================================
// 2. 排序规则定义区
// ==========================================
enum class SortType(val title: String) {
    DEFAULT("默认顺序 (最新添加)"),
    PRICE_ASC("价格 (由低到高)"),
    PRICE_DESC("价格 (由高到低)"),
    TIME_ASC("陪伴时长 (由短到长)"),
    TIME_DESC("陪伴时长 (由长到短)")
}

// ==========================================
// 3. 界面与主逻辑区
// ==========================================

sealed class Screen {
    object Home : Screen()
    data class AddEdit(val itemToEdit: GuiWuItem? = null) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "guiwu-database"
        ).build()
        val dao = db.guiwuDao()

        setContent {
            // 👇 1. 安装“光线传感器”：探测你的 iQOO 手机当前是不是深色模式
            val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

            // 👇 2. 准备两套衣服：如果是深色模式就穿黑衣服，否则穿白衣服
            val myColorScheme = if (isDarkTheme) {
                darkColorScheme()
            } else {
                lightColorScheme()
            }

            // 👇 3. 把智能衣柜交给系统
            MaterialTheme(colorScheme = myColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GuiWuBenApp(dao)
                }
            }
        }
    }
}

@Composable
fun GuiWuBenApp(dao: GuiWuDao) {
    val items by remember { dao.getAllItems() }.collectAsState(initial = emptyList())
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val coroutineScope = rememberCoroutineScope()

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                items = items,
                onNavigateToAdd = { currentScreen = Screen.AddEdit() },
                onNavigateToEdit = { item -> currentScreen = Screen.AddEdit(item) },
                onDelete = { item ->
                    coroutineScope.launch { dao.deleteItem(item) }
                }
            )
        }
        is Screen.AddEdit -> {
            AddEditScreen(
                itemToEdit = screen.itemToEdit,
                onSave = { newItem ->
                    coroutineScope.launch { dao.insertItem(newItem) }
                    currentScreen = Screen.Home
                },
                onCancel = { currentScreen = Screen.Home }
            )
        }
    }
}

// =============== UI 界面区 ===============

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    items: List<GuiWuItem>,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (GuiWuItem) -> Unit,
    onDelete: (GuiWuItem) -> Unit
) {
    var itemToManage by remember { mutableStateOf<GuiWuItem?>(null) }

    // 排序状态记录
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

            // --- 标题栏与排序菜单 ---
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
                                        color = if (sortType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
            // 👇 --- 新增：合计数据看板 --- 👇
            if (items.isNotEmpty()) {
                // 1. 在后台把总账算好
                val totalAssets = items.sumOf { it.price }
                val totalDailyCost = items.sumOf { item ->
                    val daysOwned = ChronoUnit.DAYS.between(item.date, LocalDate.now())
                    if (daysOwned > 0) item.price / daysOwned else item.price
                }

                // 2. 画一张颜色不一样的专属高级卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("当前总资产", style = MaterialTheme.typography.labelLarge)
                        Text("¥ ${String.format("%.2f", totalAssets)}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("整体日均成本：${String.format("%.2f", totalDailyCost)} 元/天", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            // 👆 --- 新增结束 --- 👆

            // --- 核心排序魔法 ---
            val sortedItems = remember(items, sortType) {
                when (sortType) {
                    SortType.DEFAULT -> items
                    SortType.PRICE_ASC -> items.sortedBy { it.price }
                    SortType.PRICE_DESC -> items.sortedByDescending { it.price }
                    SortType.TIME_ASC -> items.sortedByDescending { it.date }
                    SortType.TIME_DESC -> items.sortedBy { it.date }
                }
            }

            // --- 列表展示区 ---
            if (items.isEmpty()) {
                Text("还没有记录任何物品哦，点击右下角加号添加吧！", color = MaterialTheme.colorScheme.secondary)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 这里使用的是排序后的 sortedItems
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
                                Text("购入价格: ${item.price} 元 (日均: ${String.format("%.2f", dailyCost)} 元/天)")
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

@Composable
fun AddEditScreen(
    itemToEdit: GuiWuItem?,
    onSave: (GuiWuItem) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var price by remember { mutableStateOf(itemToEdit?.price?.toString() ?: "") }
    var selectedDate by remember { mutableStateOf(itemToEdit?.date ?: LocalDate.now()) }
    var errorMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
        Text(if (itemToEdit == null) "添加新物品" else "修改物品", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("物品名称") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("购入价格 (元)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("购买日期", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))

        DateDropdowns(
            selectedDate = selectedDate,
            onDateChange = { selectedDate = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("取消")
            }
            Button(
                onClick = {
                    val parsedPrice = price.toDoubleOrNull()
                    if (name.isBlank() || parsedPrice == null) {
                        errorMessage = "请填写正确的名称和数字格式的价格！"
                    } else {
                        val newItem = GuiWuItem(
                            id = itemToEdit?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            price = parsedPrice,
                            date = selectedDate
                        )
                        onSave(newItem)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("保存")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDropdowns(selectedDate: LocalDate, onDateChange: (LocalDate) -> Unit) {
    val currentYear = LocalDate.now().year
    val years = (2000..currentYear).toList()
    val months = (1..12).toList()
    val maxDaysInMonth = YearMonth.of(selectedDate.year, selectedDate.monthValue).lengthOfMonth()
    val days = (1..maxDaysInMonth).toList()

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SimpleDropdown("年", years, selectedDate.year, { y ->
            val newMaxDays = YearMonth.of(y, selectedDate.monthValue).lengthOfMonth()
            val newDay = if (selectedDate.dayOfMonth > newMaxDays) newMaxDays else selectedDate.dayOfMonth
            onDateChange(LocalDate.of(y, selectedDate.monthValue, newDay))
        }, Modifier.weight(1f))

        SimpleDropdown("月", months, selectedDate.monthValue, { m ->
            val newMaxDays = YearMonth.of(selectedDate.year, m).lengthOfMonth()
            val newDay = if (selectedDate.dayOfMonth > newMaxDays) newMaxDays else selectedDate.dayOfMonth
            onDateChange(LocalDate.of(selectedDate.year, m, newDay))
        }, Modifier.weight(1f))

        SimpleDropdown("日", days, selectedDate.dayOfMonth, { d ->
            onDateChange(LocalDate.of(selectedDate.year, selectedDate.monthValue, d))
        }, Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SimpleDropdown(
    label: String,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedItem.toString(),
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(item.toString()) }, onClick = {
                    onItemSelected(item)
                    expanded = false
                })
            }
        }
    }
}