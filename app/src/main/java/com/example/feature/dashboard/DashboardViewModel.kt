package com.example.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entity.ExerciseEntity
import com.example.core.database.entity.GroceryItemEntity
import com.example.core.database.entity.MedicalFolderEntity
import com.example.core.database.entity.MedicineScheduleEntity
import com.example.core.database.entity.NutritionEntryEntity
import com.example.core.database.entity.ReminderEntity
import com.example.core.database.entity.TaskEntity
import com.example.core.ui.components.startOfDayMillis
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.GroceryRepository
import com.example.data.repository.MedicalRecordsRepository
import com.example.data.repository.MedicineRepository
import com.example.data.repository.NutritionRepository
import com.example.data.repository.ReminderRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class DashboardOverviewState(
    val activeMedicinesCount: Int = 0,
    val activeExercisesCount: Int = 0,
    val activeTasksCount: Int = 0,
    val pendingGroceryCount: Int = 0,
    val medicalFoldersCount: Int = 0,
    val nutritionTodayCount: Int = 0,
    val upcomingSchedules: List<Pair<String, MedicineScheduleEntity>> = emptyList(),
    val upcomingExercises: List<ExerciseEntity> = emptyList(),
    val upcomingTasks: List<TaskEntity> = emptyList(),
    val pendingGroceryItems: List<GroceryItemEntity> = emptyList(),
    val recentMedicalFolders: List<MedicalFolderEntity> = emptyList(),
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val totalBalance: Double = 0.0,
    val upcomingNutrition: List<NutritionEntryEntity> = emptyList(),
    val upcomingReminders: List<ReminderEntity> = emptyList()
)

class DashboardViewModel(
    medicineRepo: MedicineRepository,
    exerciseRepo: ExerciseRepository,
    taskRepo: TaskRepository,
    groceryRepo: GroceryRepository,
    medicalRepo: MedicalRecordsRepository,
    walletRepo: WalletRepository,
    nutritionRepo: NutritionRepository,
    reminderRepo: ReminderRepository
) : ViewModel() {

    private val healthOverviewFlow = combine(
        medicineRepo.allMedicines,
        medicineRepo.activeSchedules,
        exerciseRepo.allExercises,
        medicalRepo.allFolders,
        nutritionRepo.allEntries
    ) { meds, schedules, exercises, folders, nutrition ->
        val medMap = meds.associateBy { it.id }
        val activeScheds = schedules.mapNotNull { s ->
            val med = medMap[s.medicineId]
            if (med != null && !med.isCompleted) med.name to s else null
        }.take(3)
        val todayStart = startOfDayMillis()
        val todayEnd = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        HealthPart(
            activeMedsCount = meds.count { !it.isCompleted },
            activeExercisesCount = exercises.count { !it.isCompleted },
            medicalFoldersCount = folders.size,
            nutritionTodayCount = nutrition.count { it.mealDateMillis in todayStart until todayEnd },
            upcomingScheds = activeScheds,
            upcomingExercises = exercises.filter { !it.isCompleted }.take(3),
            recentFolders = folders.take(3),
            upcomingNutrition = nutrition.filter { !it.isCompleted }.take(3)
        )
    }

    val overviewState: StateFlow<DashboardOverviewState> = combine(
        healthOverviewFlow,
        taskRepo.allTasks,
        groceryRepo.allItems,
        walletRepo.allTransactions,
        reminderRepo.allReminders
    ) { health, tasks, groceryItems, txs, reminders ->
        val nowCal = Calendar.getInstance()
        val curMonth = nowCal.get(Calendar.MONTH)
        val curYear = nowCal.get(Calendar.YEAR)
        val monthTxs = txs.filter { tx ->
            val c = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
            c.get(Calendar.MONTH) == curMonth && c.get(Calendar.YEAR) == curYear
        }
        val mIncome = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val mExpense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalIncome = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val activeTasks = tasks.filter { !it.isCompleted }
        val pendingGrocery = groceryItems.filter { !it.isPurchased }
        val enabledReminders = reminders
            .filter { it.isEnabled }
            .sortedBy { it.triggerTimeMillis }
            .take(5)

        DashboardOverviewState(
            activeMedicinesCount = health.activeMedsCount,
            activeExercisesCount = health.activeExercisesCount,
            activeTasksCount = activeTasks.size,
            pendingGroceryCount = pendingGrocery.size,
            medicalFoldersCount = health.medicalFoldersCount,
            nutritionTodayCount = health.nutritionTodayCount,
            upcomingSchedules = health.upcomingScheds,
            upcomingExercises = health.upcomingExercises,
            upcomingTasks = activeTasks.take(3),
            pendingGroceryItems = pendingGrocery.take(4),
            recentMedicalFolders = health.recentFolders,
            monthlyIncome = mIncome,
            monthlyExpense = mExpense,
            totalBalance = totalIncome - totalExpense,
            upcomingNutrition = health.upcomingNutrition,
            upcomingReminders = enabledReminders
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardOverviewState()
    )

    private data class HealthPart(
        val activeMedsCount: Int,
        val activeExercisesCount: Int,
        val medicalFoldersCount: Int,
        val nutritionTodayCount: Int,
        val upcomingScheds: List<Pair<String, MedicineScheduleEntity>>,
        val upcomingExercises: List<ExerciseEntity>,
        val recentFolders: List<MedicalFolderEntity>,
        val upcomingNutrition: List<NutritionEntryEntity>
    )
}
