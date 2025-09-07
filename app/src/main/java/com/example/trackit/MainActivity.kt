package com.example.trackit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.trackit.ui.theme.TrackItTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

import androidx.compose.ui.res.painterResource

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
) {
    object Home : BottomNavItem(
        route = "home",
        label = "Home",
        icon = { Icon(Icons.Default.Home, contentDescription = "Home") }
    )

    object AllExpenses : BottomNavItem(
        route = "all_expenses",
        label = "All Expenses",
        icon = { Icon(painterResource(id = R.drawable.ic_all_expenses_list), contentDescription = "All Expenses") }
    )
}

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrackItTheme(darkTheme = true) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ExpenseViewModel) {
    val navController = rememberNavController()
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != "splash") {
                AppBottomBar(navController = navController)
            }
        },
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != "splash") {
                FloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AppNavigation(navController = navController, viewModel = viewModel)
        }

        if (showAddExpenseDialog) {
            AddExpenseDialog(
                onAddExpense = { description, amount ->
                    viewModel.addExpense(description, amount)
                    showAddExpenseDialog = false
                },
                onDismiss = { showAddExpenseDialog = false }
            )
        }
    }
}

@Composable
fun AppBottomBar(navController: NavController) {
    val items = listOf(BottomNavItem.Home, BottomNavItem.AllExpenses)
    BottomAppBar(
        actions = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            items.forEach { item ->
                NavigationBarItem(
                    icon = item.icon,
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    alwaysShowLabel = true,
                    onClick = {
                        navController.navigate(item.route) {
                            navController.graph.startDestinationRoute?.let { route -> popUpTo(route) { saveState = true } }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
    )
}

@Composable
fun AppNavigation(navController: NavHostController, viewModel: ExpenseViewModel) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = navController)
        }
        composable(BottomNavItem.Home.route) {
            val budget by viewModel.budget.collectAsState()
            val total by viewModel.totalExpenses.collectAsState()
            val balance by viewModel.balance.collectAsState()
            val recentExpenses by viewModel.recentExpenses.collectAsState()
            val searchText by viewModel.searchText.collectAsState()
            val searchedExpensesByMonth by viewModel.searchedExpensesByMonth.collectAsState()

            HomeScreen(
                budget = budget,
                total = total,
                balance = balance,
                recentExpenses = recentExpenses,
                searchText = searchText,
                searchedExpensesByMonth = searchedExpensesByMonth,
                onSearchTextChange = viewModel::onSearchTextChange,
                onSetBudget = viewModel::setBudget,
                onDeleteExpense = viewModel::deleteExpense
            )
        }
        composable(BottomNavItem.AllExpenses.route) { AllExpensesScreen(viewModel = viewModel) }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(key1 = true) {
        delay(3000L)
        navController.navigate(BottomNavItem.Home.route) {
            popUpTo("splash") { inclusive = true }
        }
    }
    SplashScreenContent()
}

@Composable
fun SplashScreenContent() {
    val myCustomFont = FontFamily(
        Font(R.font.epundaslab_variablefont_wght, FontWeight.Normal)
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TrackIt",
            style = TextStyle(
                fontFamily = myCustomFont,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                color = Color.White
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    budget: Double,
    total: Double,
    balance: Double,
    recentExpenses: List<Expense>,
    searchText: String,
    searchedExpensesByMonth: Map<String, List<Expense>>,
    onSearchTextChange: (String) -> Unit,
    onSetBudget: (Double) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    val myCustomFont = FontFamily(
        Font(R.font.epundaslab_variablefont_wght, FontWeight.Normal)
    )

    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showUpdateBudgetDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "TrackIt",
            style = TextStyle(
                fontFamily = myCustomFont,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = searchText.isBlank()) {
            Column {
                BudgetSummaryCard(
                    budget = budget,
                    total = total,
                    balance = balance,
                    onEditClick = { showUpdateBudgetDialog = true }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recent Expenses",
                    style = TextStyle(
                        fontFamily = myCustomFont,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        LazyColumn {
            if (searchText.isBlank()) {
                items(recentExpenses, key = { it.id }) { expense ->
                    ExpenseItem(
                        expense = expense,
                        onLongPress = { expenseToDelete = expense }
                    )
                }
            } else {
                searchedExpensesByMonth.forEach { (month, expensesInMonth) ->
                    stickyHeader {
                        Text(
                            text = month,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(vertical = 8.dp)
                        )
                    }
                    items(expensesInMonth, key = { it.id }) { expense ->
                        ExpenseItem(
                            expense = expense,
                            onLongPress = { expenseToDelete = expense }
                        )
                    }
                }
            }
        }
    }

    if (expenseToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDeleteExpense(expenseToDelete!!)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }

    if (showUpdateBudgetDialog) {
        UpdateBudgetDialog(
            currentBudget = budget,
            onSetBudget = {
                onSetBudget(it)
                showUpdateBudgetDialog = false
            },
            onDismiss = { showUpdateBudgetDialog = false }
        )
    }
}

@Composable
fun BudgetSummaryCard(budget: Double, total: Double, balance: Double, onEditClick: () -> Unit) {
    val myCustomFont = FontFamily(
        Font(R.font.epundaslab_variablefont_wght, FontWeight.Normal)
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Budget",
                    style = TextStyle(
                        fontFamily = myCustomFont,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Budget")
                }
            }
            Text("₹${String.format("%.2f", budget)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Money Left",
                style = TextStyle(
                    fontFamily = myCustomFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            )
            Text("₹${String.format("%.2f", balance)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            val progress = if (budget > 0) (total / budget).toFloat() else 0f
            PercentageProgressBar(progress = progress)
        }
    }
}

@Composable
fun PercentageProgressBar(progress: Float) {
    val progressColor = MaterialTheme.colorScheme.primary

    val luminance = (0.299 * progressColor.red + 0.587 * progressColor.green + 0.114 * progressColor.blue)
    val textColor = if (luminance > 0.5) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            color = progressColor
        )
        Text(
            text = "${(progress * 100).roundToInt()}%",
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AllExpensesScreen(viewModel: ExpenseViewModel) {
    val myCustomFont = FontFamily(
        Font(R.font.epundaslab_variablefont_wght, FontWeight.Normal)
    )
    val expensesByMonth by viewModel.expensesByMonth.collectAsState()
    val monthList = expensesByMonth.keys.toList()
    var selectedMonth by remember { mutableStateOf(monthList.firstOrNull()) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(monthList) {
        if (selectedMonth == null || selectedMonth !in monthList) {
            selectedMonth = monthList.firstOrNull()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "All Expenses",
            style = TextStyle(
                fontFamily = myCustomFont,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (monthList.isNotEmpty() && selectedMonth != null) {
            MonthSelector(
                months = monthList,
                selectedMonth = selectedMonth!!,
                onMonthSelected = { selectedMonth = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                val expensesForMonth = expensesByMonth[selectedMonth] ?: emptyList()
                items(expensesForMonth, key = { it.id }) { expense ->
                    ExpenseItem(
                        expense = expense,
                        onLongPress = { expenseToDelete = expense }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No expenses yet!")
            }
        }
    }

    if (expenseToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteExpense(expenseToDelete!!)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSelector(months: List<String>, selectedMonth: String, onMonthSelected: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = !isExpanded }) {
        OutlinedTextField(
            value = selectedMonth,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Month") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            months.forEach { month ->
                DropdownMenuItem(text = { Text(month) }, onClick = {
                    onMonthSelected(month)
                    isExpanded = false
                })
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, onLongPress: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isExpanded = !isExpanded },
                    onLongPress = { onLongPress() }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = expense.description, fontWeight = FontWeight.Medium)
                Text(text = "₹${String.format("%.2f", expense.amount)}", color = Color.Gray)
            }
            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = "Added on: ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a").format(java.util.Date(expense.id))}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun AddExpenseDialog(onAddExpense: (String, Double) -> Unit, onDismiss: () -> Unit) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Expense") },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amountValue = amount.toDoubleOrNull()
                if (description.isNotBlank() && amountValue != null) {
                    onAddExpense(description, amountValue)
                }
            }) { Text("Add") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Expense") },
        text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
        confirmButton = { Button(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun UpdateBudgetDialog(currentBudget: Double, onSetBudget: (Double) -> Unit, onDismiss: () -> Unit) {
    var budgetInput by remember { mutableStateOf(if (currentBudget == 0.0) "" else currentBudget.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Budget") },
        text = {
            OutlinedTextField(
                value = budgetInput,
                onValueChange = { budgetInput = it },
                label = { Text("Enter New Budget") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(onClick = {
                val budgetValue = budgetInput.toDoubleOrNull() ?: 0.0
                onSetBudget(budgetValue)
            }) { Text("Update") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview(showBackground = true)
@Composable
fun BudgetCardPreview() {
    TrackItTheme(darkTheme = true) {
        BudgetSummaryCard(
            budget = 20000.0,
            total = 7500.0,
            balance = 12500.0,
            onEditClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    TrackItTheme {
        SplashScreenContent()
    }
}

@Preview(showBackground = true, name = "HomeScreen Preview")
@Composable
fun HomeScreenPreview() {
    TrackItTheme(darkTheme = true) {
        HomeScreen(
            budget = 50000.0,
            total = 15000.0,
            balance = 35000.0,
            recentExpenses = listOf(
                Expense(description = "Groceries", amount = 2500.0),
                Expense(description = "Dinner", amount = 1200.0)
            ),
            searchText = "",
            searchedExpensesByMonth = emptyMap(),
            onSearchTextChange = {},
            onSetBudget = {},
            onDeleteExpense = {}
        )
    }
}