// Expense.kt

package com.example.trackit

data class Expense(
    val id: Long = System.currentTimeMillis(),
    val description: String,
    val amount: Double
)