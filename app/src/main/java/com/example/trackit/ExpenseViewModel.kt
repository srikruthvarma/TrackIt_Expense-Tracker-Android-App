package com.example.trackit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("ExpenseTrackerPrefs", 0)
    private val gson = Gson()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _budget = MutableStateFlow(0.0)
    val budget: StateFlow<Double> = _budget.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    init {
        loadData()
    }

    val recentExpenses: StateFlow<List<Expense>> = _expenses.map { list ->
        list.sortedByDescending { it.id }.take(4)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val searchedExpensesByMonth: StateFlow<Map<String, List<Expense>>> = combine(_expenses, _searchText) { expenses, text ->
        if (text.isBlank()) {
            emptyMap()
        } else {
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            expenses
                .filter { it.description.contains(text, ignoreCase = true) }
                .sortedByDescending { it.id }
                .groupBy { expense ->
                    dateFormat.format(Calendar.getInstance().apply { timeInMillis = expense.id }.time)
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val expensesByMonth: StateFlow<Map<String, List<Expense>>> = _expenses.map { expenses ->
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        expenses.sortedByDescending { it.id }.groupBy { expense ->
            dateFormat.format(Calendar.getInstance().apply { timeInMillis = expense.id }.time)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    private fun loadData() {
        val savedBudget = sharedPreferences.getFloat("budget", 0.0f).toDouble()
        _budget.value = savedBudget

        val jsonExpenses = sharedPreferences.getString("expenses", null)
        if (jsonExpenses != null) {
            val type = object : TypeToken<List<Expense>>() {}.type
            _expenses.value = gson.fromJson(jsonExpenses, type)
        }
    }

    private fun saveExpenses() {
        val jsonExpenses = gson.toJson(_expenses.value)
        sharedPreferences.edit().putString("expenses", jsonExpenses).apply()
    }

    private fun saveBudget() {
        sharedPreferences.edit().putFloat("budget", _budget.value.toFloat()).apply()
    }

    val totalExpenses: StateFlow<Double> = _expenses.map { list ->
        list.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val balance: StateFlow<Double> = combine(budget, totalExpenses) { budget, total ->
        budget - total
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun addExpense(description: String, amount: Double) {
        val newExpense = Expense(description = description, amount = amount)
        _expenses.value = _expenses.value + newExpense
        saveExpenses()
    }

    fun deleteExpense(expense: Expense) {
        _expenses.value = _expenses.value.filterNot { it.id == expense.id }
        saveExpenses()
    }

    fun setBudget(newBudget: Double) {
        _budget.value = newBudget
        saveBudget()
    }
}