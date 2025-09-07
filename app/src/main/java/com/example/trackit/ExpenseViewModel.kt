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

enum class BudgetPeriod {
    WEEKLY, MONTHLY, YEARLY, NONE
}

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("ExpenseTrackerPrefs", 0)
    private val gson = Gson()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _budget = MutableStateFlow(0.0)
    val budget: StateFlow<Double> = _budget.asStateFlow()

    private val _budgetPeriod = MutableStateFlow(BudgetPeriod.NONE)
    val budgetPeriod: StateFlow<BudgetPeriod> = _budgetPeriod.asStateFlow()

    private val _budgetStartDate = MutableStateFlow(0L)

    private val _warningHasBeenShown = MutableStateFlow(false)

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    init {
        loadData()
    }

    val totalExpensesForPeriod: StateFlow<Double> = combine(_expenses, _budgetStartDate) { expenses, startDate ->
        if (startDate == 0L) 0.0 else expenses.filter { it.id >= startDate }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val showBudgetWarningPopup: StateFlow<Boolean> = combine(
        budget,
        totalExpensesForPeriod,
        _warningHasBeenShown
    ) { budget, total, hasBeenShown ->
        budget > 0 && (total / budget) > 0.8 && !hasBeenShown
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

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

    val balance: StateFlow<Double> = combine(budget, totalExpensesForPeriod) { budget, total ->
        budget - total
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    private fun loadData() {
        val savedBudget = sharedPreferences.getFloat("budget", 0.0f).toDouble()
        val savedPeriod = BudgetPeriod.valueOf(sharedPreferences.getString("budget_period", BudgetPeriod.NONE.name)!!)
        val savedStartDate = sharedPreferences.getLong("budget_start_date", 0L)

        if (isCurrentPeriod(savedStartDate, savedPeriod)) {
            _budget.value = savedBudget
            _budgetPeriod.value = savedPeriod
            _budgetStartDate.value = savedStartDate
            val warningShownKey = "warning_shown_${savedStartDate}"
            _warningHasBeenShown.value = sharedPreferences.getBoolean(warningShownKey, false)
        } else {
            setBudget(0.0, BudgetPeriod.NONE)
        }

        val jsonExpenses = sharedPreferences.getString("expenses", null)
        if (jsonExpenses != null) {
            val type = object : TypeToken<List<Expense>>() {}.type
            _expenses.value = gson.fromJson(jsonExpenses, type)
        }
    }

    private fun isCurrentPeriod(startDate: Long, period: BudgetPeriod): Boolean {
        if (startDate == 0L || period == BudgetPeriod.NONE) return false

        val now = Calendar.getInstance()
        val start = Calendar.getInstance().apply { timeInMillis = startDate }

        return when (period) {
            BudgetPeriod.WEEKLY -> now.get(Calendar.WEEK_OF_YEAR) == start.get(Calendar.WEEK_OF_YEAR) && now.get(Calendar.YEAR) == start.get(Calendar.YEAR)
            BudgetPeriod.MONTHLY -> now.get(Calendar.MONTH) == start.get(Calendar.MONTH) && now.get(Calendar.YEAR) == start.get(Calendar.YEAR)
            BudgetPeriod.YEARLY -> now.get(Calendar.YEAR) == start.get(Calendar.YEAR)
            BudgetPeriod.NONE -> false
        }
    }

    private fun saveExpenses() {
        val jsonExpenses = gson.toJson(_expenses.value)
        sharedPreferences.edit().putString("expenses", jsonExpenses).apply()
    }

    private fun saveBudget() {
        val editor = sharedPreferences.edit()
        editor.putFloat("budget", _budget.value.toFloat())
        editor.putString("budget_period", _budgetPeriod.value.name)
        editor.putLong("budget_start_date", _budgetStartDate.value)
        editor.apply()
    }

    fun setBudget(newBudget: Double, period: BudgetPeriod) {
        val oldStartDate = _budgetStartDate.value
        val warningShownKey = "warning_shown_${oldStartDate}"
        sharedPreferences.edit().remove(warningShownKey).apply()

        _budget.value = newBudget
        _budgetPeriod.value = period

        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)

        _budgetStartDate.value = when (period) {
            BudgetPeriod.WEEKLY -> {
                now.set(Calendar.DAY_OF_WEEK, now.firstDayOfWeek)
                now.timeInMillis
            }
            BudgetPeriod.MONTHLY -> {
                now.set(Calendar.DAY_OF_MONTH, 1)
                now.timeInMillis
            }
            BudgetPeriod.YEARLY -> {
                now.set(Calendar.DAY_OF_YEAR, 1)
                now.timeInMillis
            }
            BudgetPeriod.NONE -> 0L
        }
        _warningHasBeenShown.value = false
        saveBudget()
    }

    fun dismissBudgetWarning() {
        val warningShownKey = "warning_shown_${_budgetStartDate.value}"
        sharedPreferences.edit().putBoolean(warningShownKey, true).apply()
        _warningHasBeenShown.value = true
    }

    fun deleteBudget() {
        setBudget(0.0, BudgetPeriod.NONE)
    }

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
}