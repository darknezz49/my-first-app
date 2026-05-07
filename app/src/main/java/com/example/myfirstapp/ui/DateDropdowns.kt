package com.example.myfirstapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

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
