package com.example.myfirstapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.PieValueFormatter
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart

private val pieColors = listOf(
    Color(0xFF2196F3), // Blue
    Color(0xFF4CAF50), // Green
    Color(0xFFFF9800), // Orange
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFF5722), // Deep Orange
    Color(0xFF607D8B), // Blue Grey
    Color(0xFF795548), // Brown
)

@Composable
fun StatPieTab(items: List<com.example.myfirstapp.data.GuiWuItem>) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无物品数据", color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    val pieData = remember(items) { getPieData(items) }
    val modelProducer = remember { PieChartModelProducer() }

    LaunchedEffect(pieData) {
        if (pieData.isNotEmpty()) {
            modelProducer.runTransaction {
                pieSeries { series(pieData.map { it.price }) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        PieChartHost(
            chart = rememberPieChart(
                sliceProvider = PieChart.SliceProvider.series(
                    pieColors.map { color ->
                        PieChart.Slice(
                            fill = Fill(color),
                            strokeFill = Fill(Color.White),
                            strokeThickness = 1.dp,
                            label = PieChart.SliceLabel.Inside(
                                TextComponent(
                                    TextStyle(fontSize = 11.sp, color = Color.White)
                                )
                            ),
                        )
                    }
                ),
                valueFormatter = PieValueFormatter { _, value, _ ->
                    val total = pieData.sumOf { it.price }
                    val pct = if (total > 0) (value / total * 100).toInt() else 0
                    "${pct}%"
                },
            ),
            modelProducer = modelProducer,
            modifier = Modifier.height(280.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        pieData.forEachIndexed { index, slice ->
            val color = pieColors[index % pieColors.size]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${slice.name}  ¥${"%.2f".format(slice.price)}  (${"%.1f".format(slice.percentage)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
