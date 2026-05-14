package com.example.myfirstapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

private val MonthLabelKey = ExtraStore.Key<List<String>>()

@Composable
fun StatTrendTab(items: List<com.example.myfirstapp.data.GuiWuItem>) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无物品数据", color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    val trends = remember(items) { getMonthlyTrends(items) }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(trends) {
        if (trends.isNotEmpty()) {
            modelProducer.runTransaction {
                columnSeries {
                    series(trends.map { it.totalPrice })
                }
                lineSeries {
                    series(trends.map { it.itemCount.toDouble() })
                }
                extras {
                    it[MonthLabelKey] = trends.map { t -> t.month }
                }
            }
        }
    }

    val bottomAxisValueFormatter = remember {
        CartesianValueFormatter { context, x, _ ->
            val labels = context.model.extraStore[MonthLabelKey]
            val index = x.toInt()
            if (labels != null && index in labels.indices) labels[index] else ""
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                rememberLineCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = bottomAxisValueFormatter,
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxSize().height(320.dp),
        )
    }
}
