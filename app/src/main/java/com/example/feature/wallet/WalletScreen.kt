package com.example.feature.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.database.entity.FinanceTransactionEntity
import com.example.core.ui.components.BarChartEntry
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.DatePickerField
import com.example.core.ui.components.MyCareEmptyState
import com.example.core.ui.components.MyCareTopBar
import com.example.core.ui.components.MyCareSearchBar
import com.example.core.ui.components.SimpleBarChart
import com.example.core.ui.components.formatDateShort
import java.text.DateFormatSymbols

@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Overview, 1 = Transactions, 2 = Calculator
    var editorType by remember { mutableStateOf<String?>(null) }
    var editingTx by remember { mutableStateOf<FinanceTransactionEntity?>(null) }
    var txToDelete by remember { mutableStateOf<FinanceTransactionEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        MyCareTopBar(title = stringResource(R.string.feature_wallet_title), onBack = onBack)
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.wallet_overview)) },
                modifier = Modifier.testTag("tab_wallet_overview")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.wallet_transactions)) },
                modifier = Modifier.testTag("tab_wallet_transactions")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(stringResource(R.string.wallet_calculator)) },
                modifier = Modifier.testTag("tab_wallet_calculator")
            )
        }

        when (selectedTab) {
            0 -> WalletOverviewTab(
                state = state,
                viewModel = viewModel,
                onAddIncome = { editingTx = null; editorType = "INCOME" },
                onAddExpense = { editingTx = null; editorType = "EXPENSE" }
            )
            1 -> WalletTransactionsTab(
                state = state,
                viewModel = viewModel,
                onAddIncome = { editingTx = null; editorType = "INCOME" },
                onAddExpense = { editingTx = null; editorType = "EXPENSE" },
                onEdit = { editingTx = it; editorType = it.type },
                onDelete = { txToDelete = it }
            )
            2 -> BuiltInCalculatorTab()
        }
    }

    editorType?.let { txType ->
        TransactionEditorSheet(
            type = txType,
            existing = editingTx,
            categories = state.categories.filter { it.type == txType }.map { it.name },
            onAddCategory = { name -> viewModel.addCustomCategory(name, txType) },
            onDismiss = { editorType = null; editingTx = null },
            onSave = { title, amount, cat, date, payMethod, notes ->
                viewModel.saveTransaction(editingTx, txType, title, amount, cat, date, payMethod, notes) {
                    editorType = null
                    editingTx = null
                }
            }
        )
    }

    txToDelete?.let { tx ->
        ConfirmDeleteDialog(
            onConfirm = {
                viewModel.deleteTransaction(tx)
                txToDelete = null
            },
            onDismiss = { txToDelete = null }
        )
    }
}

@Composable
private fun WalletOverviewTab(
    state: WalletUiState,
    viewModel: WalletViewModel,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit
) {
    val months = remember { DateFormatSymbols().shortMonths }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Overall Balance Card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(R.string.wallet_balance),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            String.format("%.2f", state.balance),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (state.balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(R.string.wallet_total_income), style = MaterialTheme.typography.labelSmall)
                                Text(String.format("%.2f", state.totalIncome), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.wallet_total_expense), style = MaterialTheme.typography.labelSmall)
                                Text(String.format("%.2f", state.totalExpense), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onAddIncome, modifier = Modifier.weight(1f).testTag("btn_add_income")) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.wallet_add_income), maxLines = 1)
                            }
                            OutlinedButton(onClick = onAddExpense, modifier = Modifier.weight(1f).testTag("btn_add_expense")) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.wallet_add_expense), maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Period Selector & Month/Year Filter
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "DAILY" to R.string.wallet_view_daily,
                        "WEEKLY" to R.string.wallet_view_weekly,
                        "MONTHLY" to R.string.wallet_view_monthly,
                        "YEARLY" to R.string.wallet_view_yearly
                    ).forEach { (key, res) ->
                        FilterChip(
                            selected = state.periodMode == key,
                            onClick = { viewModel.setPeriodMode(key) },
                            label = { Text(stringResource(res)) }
                        )
                    }
                }
            }

            if (state.periodMode == "MONTHLY" || state.periodMode == "YEARLY") {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (state.periodMode == "MONTHLY") {
                                if (state.selectedMonth == 0) {
                                    viewModel.setMonth(11)
                                    viewModel.setYear(state.selectedYear - 1)
                                } else {
                                    viewModel.setMonth(state.selectedMonth - 1)
                                }
                            } else {
                                viewModel.setYear(state.selectedYear - 1)
                            }
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = null)
                        }
                        Text(
                            text = if (state.periodMode == "MONTHLY") {
                                "${months.getOrElse(state.selectedMonth) { "" }} ${state.selectedYear}"
                            } else "${state.selectedYear}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = {
                            if (state.periodMode == "MONTHLY") {
                                if (state.selectedMonth == 11) {
                                    viewModel.setMonth(0)
                                    viewModel.setYear(state.selectedYear + 1)
                                } else {
                                    viewModel.setMonth(state.selectedMonth + 1)
                                }
                            } else {
                                viewModel.setYear(state.selectedYear + 1)
                            }
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            // Period Summary & Bar Chart
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "${stringResource(R.string.wallet_monthly_savings)}: ${String.format("%.2f", state.periodSavings)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        SimpleBarChart(
                            entries = listOf(
                                BarChartEntry("Income", state.periodIncome.toFloat(), MaterialTheme.colorScheme.primary),
                                BarChartEntry("Expense", state.periodExpense.toFloat(), MaterialTheme.colorScheme.error),
                                BarChartEntry("Savings", state.periodSavings.coerceAtLeast(0.0).toFloat(), MaterialTheme.colorScheme.tertiary)
                            )
                        )
                    }
                }
            }

            // Category Breakdown
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            stringResource(R.string.wallet_cat_breakdown),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (state.categoryExpenses.isEmpty()) {
                            Text(
                                stringResource(R.string.wallet_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val total = state.periodExpense.coerceAtLeast(1.0)
                            state.categoryExpenses.forEach { (cat, amt) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(cat, style = MaterialTheme.typography.bodyMedium)
                                        Text(String.format("%.2f (%.0f%%)", amt, amt / total * 100), style = MaterialTheme.typography.bodySmall)
                                    }
                                    LinearProgressIndicator(
                                        progress = { (amt / total).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletTransactionsTab(
    state: WalletUiState,
    viewModel: WalletViewModel,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onEdit: (FinanceTransactionEntity) -> Unit,
    onDelete: (FinanceTransactionEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddIncome, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.wallet_add_income), maxLines = 1)
                }
                OutlinedButton(onClick = onAddExpense, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.wallet_add_expense), maxLines = 1)
                }
            }
        }
        item {
            MyCareSearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearch,
                placeholder = stringResource(R.string.action_search)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "INCOME", "EXPENSE").forEach { t ->
                    FilterChip(
                        selected = state.typeFilter == t,
                        onClick = { viewModel.setTypeFilter(t) },
                        label = { Text(t) }
                    )
                }
            }
        }
        if (state.filteredTransactions.isEmpty()) {
            item {
                MyCareEmptyState(
                    icon = Icons.Default.AccountBalanceWallet,
                    message = stringResource(R.string.wallet_empty)
                )
            }
        } else {
            items(state.filteredTransactions, key = { it.id }) { tx ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${tx.category} • ${tx.paymentMethod} • ${formatDateShort(tx.dateMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (tx.notes.isNotBlank()) {
                                Text(tx.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(
                            text = (if (tx.type == "INCOME") "+" else "-") + String.format("%.2f", tx.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (tx.type == "INCOME") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        IconButton(onClick = { onEdit(tx) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { onDelete(tx) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuiltInCalculatorTab() {
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    var isError by remember { mutableStateOf(false) }

    fun appendToken(token: String) {
        isError = false
        expression += token
    }

    fun evaluateNow() {
        CalculatorEvaluator.evaluate(expression)
            .onSuccess { res ->
                resultText = res
                isError = false
            }
            .onFailure {
                isError = true
            }
    }

    val errorLabel = stringResource(R.string.calc_error)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expression.ifBlank { "0" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isError) errorLabel else resultText,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("calc_result_text")
                    )
                }
            }

            val rows = listOf(
                listOf("C", "(", ")", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("±", "0", ".", "%")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        val isOp = key in listOf("÷", "×", "-", "+", "%", "(", ")", "C")
                        FilledTonalButton(
                            onClick = {
                                when (key) {
                                    "C" -> { expression = ""; resultText = "0"; isError = false }
                                    "±" -> {
                                        expression = if (expression.startsWith("-")) expression.drop(1) else "-$expression"
                                    }
                                    else -> appendToken(key)
                                }
                            },
                            modifier = Modifier.weight(1f).height(58.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = if (isOp) {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                ButtonDefaults.filledTonalButtonColors()
                            }
                        ) {
                            Text(key, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (expression.isNotEmpty()) expression = expression.dropLast(1)
                        isError = false
                    },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(R.string.action_delete))
                }
                Button(
                    onClick = { evaluateNow() },
                    modifier = Modifier.weight(2f).height(58.dp).testTag("calc_equals_btn"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("=", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TransactionEditorSheet(
    type: String,
    existing: FinanceTransactionEntity?,
    categories: List<String>,
    onAddCategory: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, dateMillis: Long, paymentMethod: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amountStr by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: categories.firstOrNull() ?: "Other") }
    var dateMillis by remember { mutableLongStateOf(existing?.dateMillis ?: System.currentTimeMillis()) }
    var payMethod by remember { mutableStateOf(existing?.paymentMethod ?: "Cash") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var showError by remember { mutableStateOf(false) }
    var showNewCat by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(if (type == "INCOME") R.string.wallet_add_income else R.string.wallet_add_expense),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; showError = false },
                label = { Text(stringResource(R.string.wallet_title) + " *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("wallet_title_input")
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it; showError = false },
                label = { Text(stringResource(R.string.wallet_amount) + " *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("wallet_amount_input")
            )

            Text(stringResource(R.string.label_category), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                }
                AssistChip(onClick = { showNewCat = true }, label = { Text("+ " + stringResource(R.string.wallet_custom_cat)) })
            }

            DatePickerField(
                label = stringResource(R.string.label_date),
                dateMillis = dateMillis,
                onDateSelected = { dateMillis = it },
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.wallet_payment_method), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Cash", "Card", "Mobile Banking", "Bank Transfer").forEach { pm ->
                    FilterChip(selected = payMethod == pm, onClick = { payMethod = pm }, label = { Text(pm) })
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.label_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (showError) {
                Text(stringResource(R.string.label_required_error), color = MaterialTheme.colorScheme.error)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull()
                        if (title.isBlank() || amt == null || !amt.isFinite() || amt <= 0.0) {
                            showError = true
                        } else {
                            onSave(title, amt, category, dateMillis, payMethod, notes)
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("save_transaction_btn")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (showNewCat) {
        var newCat by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCat = false },
            title = { Text(stringResource(R.string.wallet_custom_cat)) },
            text = { OutlinedTextField(value = newCat, onValueChange = { newCat = it }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    if (newCat.isNotBlank()) {
                        onAddCategory(newCat.trim())
                        category = newCat.trim()
                    }
                    showNewCat = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewCat = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
