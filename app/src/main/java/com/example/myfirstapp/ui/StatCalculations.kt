package com.example.myfirstapp.ui

import com.example.myfirstapp.data.GuiWuItem
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class MonthlyTrend(
    val month: String,
    val totalPrice: Double,
    val itemCount: Int
)

data class PieSlice(
    val name: String,
    val price: Double,
    val percentage: Float
)

data class DailyCostItem(
    val name: String,
    val dailyCost: Double,
    val daysOwned: Long
)

fun getMonthlyTrends(items: List<GuiWuItem>): List<MonthlyTrend> {
    val data = mutableMapOf<String, MonthlyTrend>()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")

    for (item in items) {
        val month = item.date.format(formatter)
        val existing = data[month]
        if (existing != null) {
            data[month] = existing.copy(
                totalPrice = existing.totalPrice + item.price,
                itemCount = existing.itemCount + 1
            )
        } else {
            data[month] = MonthlyTrend(month, item.price, 1)
        }
    }

    return data.entries
        .sortedBy { it.key }
        .map { it.value }
}

fun getPieData(items: List<GuiWuItem>): List<PieSlice> {
    val total = items.sumOf { it.price }
    if (total == 0.0) return emptyList()

    val sorted = items.sortedByDescending { it.price }
    val mainItems = if (sorted.size > 8) sorted.take(7) else sorted
    val others = if (sorted.size > 8) sorted.drop(7) else emptyList()

    val result = mainItems.map { item ->
        PieSlice(
            name = item.name,
            price = item.price,
            percentage = (item.price / total * 100).toFloat()
        )
    }.toMutableList()

    if (others.isNotEmpty()) {
        val otherTotal = others.sumOf { it.price }
        result.add(
            PieSlice(
                name = "其他 (${others.size}件)",
                price = otherTotal,
                percentage = (otherTotal / total * 100).toFloat()
            )
        )
    }

    return result
}

fun getDailyCostList(items: List<GuiWuItem>): List<DailyCostItem> {
    val now = LocalDate.now()
    return items.map { item ->
        val daysOwned = ChronoUnit.DAYS.between(item.date, now)
        val dailyCost = if (daysOwned > 0) item.price / daysOwned else item.price
        DailyCostItem(
            name = item.name,
            dailyCost = dailyCost,
            daysOwned = daysOwned
        )
    }.sortedByDescending { it.dailyCost }
}
