package com.example.utilityhub.features.calculators

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.utilityhub.ui.HistoryViewModel

@Composable
fun QuickCalcScreen(historyViewModel: HistoryViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Percentage", "Shopping")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        when (selectedTab) {
            0 -> PercentageScreen(historyViewModel)
            1 -> ItemCostScreen(historyViewModel)
        }
    }
}
