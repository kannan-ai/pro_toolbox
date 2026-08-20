package com.example.utilityhub.features.text

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.utilityhub.ui.HistoryViewModel

@Composable
fun TextStudioScreen(historyViewModel: HistoryViewModel, initialTab: Int = 0) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }
    val tabs = listOf("Translate", "Rewrite")

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
            0 -> TranslatorScreen(historyViewModel)
            1 -> HumanizeScreen()
        }
    }
}
