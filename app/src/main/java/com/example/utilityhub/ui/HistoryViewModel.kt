package com.example.utilityhub.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.utilityhub.data.db.HistoryDao
import com.example.utilityhub.data.db.HistoryEntry
import com.example.utilityhub.data.prefs.ThemeManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyDao: HistoryDao,
    private val themeManager: ThemeManager
) : ViewModel() {
    val allHistory: StateFlow<List<HistoryEntry>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addHistory(type: String, input: String, result: String) {
        viewModelScope.launch {
            val isIncognito = themeManager.isIncognitoMode.first()
            if (!isIncognito) {
                historyDao.insert(HistoryEntry(type = type, input = input, result = result))
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyDao.clearHistory()
        }
    }
}
