package com.example.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entity.FinanceCategoryEntity
import com.example.core.database.entity.FinanceTransactionEntity
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class WalletUiState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val periodSavings: Double = 0.0,
    val categoryExpenses: List<Pair<String, Double>> = emptyList(),
    val filteredTransactions: List<FinanceTransactionEntity> = emptyList(),
    val categories: List<FinanceCategoryEntity> = emptyList(),
    val periodMode: String = "MONTHLY", // DAILY, WEEKLY, MONTHLY, YEARLY
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val typeFilter: String = "ALL", // ALL, INCOME, EXPENSE
    val searchQuery: String = ""
)

class WalletViewModel(
    private val walletRepo: WalletRepository
) : ViewModel() {

    private val periodMode = MutableStateFlow("MONTHLY")
    private val selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val typeFilter = MutableStateFlow("ALL")
    private val searchQuery = MutableStateFlow("")

    private val filterFlow = combine(
        periodMode, selectedMonth, selectedYear, typeFilter, searchQuery
    ) { mode, m, y, type, q -> FilterParams(mode, m, y, type, q) }

    val uiState: StateFlow<WalletUiState> = combine(
        walletRepo.allTransactions,
        walletRepo.categories,
        filterFlow
    ) { txs, cats, f ->
        val totalInc = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExp = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val now = Calendar.getInstance()
        val periodTxs = txs.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
            when (f.mode) {
                "DAILY" -> cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                "WEEKLY" -> {
                    val weekAgo = now.timeInMillis - 7 * 86_400_000L
                    tx.dateMillis >= weekAgo
                }
                "YEARLY" -> cal.get(Calendar.YEAR) == f.year
                else -> cal.get(Calendar.MONTH) == f.month && cal.get(Calendar.YEAR) == f.year
            }
        }

        val pInc = periodTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val pExp = periodTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val catBreakdown = periodTxs
            .filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }

        val visibleTxs = periodTxs.filter { tx ->
            val matchType = f.type == "ALL" || tx.type == f.type
            val matchQ = f.query.isBlank() ||
                tx.title.contains(f.query, ignoreCase = true) ||
                tx.category.contains(f.query, ignoreCase = true) ||
                tx.notes.contains(f.query, ignoreCase = true)
            matchType && matchQ
        }

        WalletUiState(
            totalIncome = totalInc,
            totalExpense = totalExp,
            balance = totalInc - totalExp,
            periodIncome = pInc,
            periodExpense = pExp,
            periodSavings = pInc - pExp,
            categoryExpenses = catBreakdown,
            filteredTransactions = visibleTxs,
            categories = cats,
            periodMode = f.mode,
            selectedMonth = f.month,
            selectedYear = f.year,
            typeFilter = f.type,
            searchQuery = f.query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WalletUiState())

    fun setPeriodMode(mode: String) { periodMode.value = mode }
    fun setMonth(month: Int) { selectedMonth.value = month }
    fun setYear(year: Int) { selectedYear.value = year }
    fun setTypeFilter(type: String) { typeFilter.value = type }
    fun setSearch(q: String) { searchQuery.value = q }

    fun saveTransaction(
        existing: FinanceTransactionEntity?,
        type: String,
        title: String,
        amount: Double,
        category: String,
        dateMillis: Long,
        paymentMethod: String,
        notes: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val entity = (existing ?: FinanceTransactionEntity(
                type = type, title = title, amount = amount, category = category
            )).copy(
                type = type,
                title = title.trim(),
                amount = amount,
                category = category,
                dateMillis = dateMillis,
                paymentMethod = paymentMethod,
                notes = notes.trim()
            )
            walletRepo.saveTransaction(entity)
            onDone()
        }
    }

    fun deleteTransaction(tx: FinanceTransactionEntity) {
        viewModelScope.launch { walletRepo.deleteTransaction(tx) }
    }

    fun addCustomCategory(name: String, type: String) {
        viewModelScope.launch { walletRepo.addCategory(name, type) }
    }

    private data class FilterParams(
        val mode: String,
        val month: Int,
        val year: Int,
        val type: String,
        val query: String
    )
}
