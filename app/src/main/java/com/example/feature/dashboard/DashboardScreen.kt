package com.example.feature.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.navigation.MyCareRoute
import com.example.core.theme.FeatureAmber
import com.example.core.theme.FeatureCoral
import com.example.core.theme.FeatureEmerald
import com.example.core.theme.FeatureIndigo
import com.example.core.theme.FeatureLime
import com.example.core.theme.FeatureSky
import com.example.core.theme.FeatureTeal
import com.example.core.theme.FeatureTravel
import com.example.core.theme.FeatureViolet
import com.example.core.ui.components.formatDateTimeShort
import com.example.core.ui.components.formatTime12h
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class FeatureCardSpec(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val accentColor: Color,
    val route: MyCareRoute,
    val testTag: String,
    val summaryProvider: (DashboardOverviewState) -> String?
)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (MyCareRoute) -> Unit
) {
    val overview by viewModel.overviewState.collectAsStateWithLifecycle()

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingRes = when (currentHour) {
        in 5..11 -> R.string.greeting_morning
        in 12..16 -> R.string.greeting_afternoon
        in 17..20 -> R.string.greeting_evening
        else -> R.string.greeting_night
    }

    val currentDateText = remember(currentHour) {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    // 8 Core Features in 2-column horizontal grid
    val mainFeatures = remember {
        listOf(
            FeatureCardSpec(
                R.string.feature_medicine_title,
                R.string.feature_medicine_desc,
                Icons.Default.Medication,
                FeatureTeal,
                MyCareRoute.MedicineList,
                "card_medicine"
            ) { if (it.activeMedicinesCount > 0) "${it.activeMedicinesCount} active" else null },
            FeatureCardSpec(
                R.string.feature_exercise_title,
                R.string.feature_exercise_desc,
                Icons.Default.FitnessCenter,
                FeatureCoral,
                MyCareRoute.ExerciseList,
                "card_exercise"
            ) { if (it.activeExercisesCount > 0) "${it.activeExercisesCount} active" else null },
            FeatureCardSpec(
                R.string.feature_task_title,
                R.string.feature_task_desc,
                Icons.Default.AddTask,
                FeatureIndigo,
                MyCareRoute.Tasks,
                "card_tasks"
            ) { if (it.activeTasksCount > 0) "${it.activeTasksCount} pending" else null },
            FeatureCardSpec(
                R.string.feature_wallet_title,
                R.string.feature_wallet_desc,
                Icons.Default.AccountBalanceWallet,
                FeatureEmerald,
                MyCareRoute.Wallet,
                "card_wallet"
            ) { if (it.monthlyIncome > 0.0 || it.monthlyExpense > 0.0) String.format("%.0f bal", it.totalBalance) else null },
            FeatureCardSpec(
                R.string.feature_grocery_title,
                R.string.feature_grocery_desc,
                Icons.Default.ShoppingCart,
                FeatureAmber,
                MyCareRoute.Grocery,
                "card_grocery"
            ) { if (it.pendingGroceryCount > 0) "${it.pendingGroceryCount} to buy" else null },
            FeatureCardSpec(
                R.string.feature_medical_records_title,
                R.string.feature_medical_records_desc,
                Icons.Default.FolderShared,
                FeatureViolet,
                MyCareRoute.MedicalRecords,
                "card_medical_records"
            ) { if (it.medicalFoldersCount > 0) "${it.medicalFoldersCount} folders" else null },
            FeatureCardSpec(
                R.string.feature_doctor_search_title,
                R.string.feature_doctor_search_desc,
                Icons.Default.LocalHospital,
                FeatureSky,
                MyCareRoute.DoctorSearch,
                "card_doctor_search"
            ) { null },
            FeatureCardSpec(
                R.string.feature_nutrition_title,
                R.string.feature_nutrition_desc,
                Icons.Default.Restaurant,
                FeatureLime,
                MyCareRoute.Nutrition,
                "card_nutrition"
            ) { if (it.nutritionTodayCount > 0) "${it.nutritionTodayCount} today" else null }
        )
    }

    val featurePairs = remember(mainFeatures) { mainFeatures.chunked(2) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("dashboard_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dynamic Local Time Greeting & MYCARE Branding Banner
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    )
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.mycare_logo),
                                    contentDescription = stringResource(R.string.app_name),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(greetingRes),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = currentDateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${stringResource(R.string.app_name)} • ${stringResource(R.string.app_tagline)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Two-Column Feature Cards Grid (8 Core Features)
            items(featurePairs) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { spec ->
                        val summary = spec.summaryProvider(overview)
                        DashboardGridFeatureCard(
                            title = stringResource(spec.titleRes),
                            subtitle = summary ?: stringResource(spec.descRes),
                            isRealSummary = summary != null,
                            icon = spec.icon,
                            accentColor = spec.accentColor,
                            onClick = { onNavigate(spec.route) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag(spec.testTag)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // More Modules Section (Matching 2-Column Grid)
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.section_more_modules),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardGridFeatureCard(
                        title = stringResource(R.string.feature_travel_title),
                        subtitle = stringResource(R.string.feature_travel_desc),
                        isRealSummary = false,
                        icon = Icons.Default.FlightTakeoff,
                        accentColor = FeatureTravel,
                        onClick = { onNavigate(MyCareRoute.Travel) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("card_travel")
                    )
                    DashboardGridFeatureCard(
                        title = stringResource(R.string.feature_emergency_title),
                        subtitle = stringResource(R.string.feature_emergency_desc),
                        isRealSummary = false,
                        icon = Icons.Default.Emergency,
                        accentColor = MaterialTheme.colorScheme.error,
                        onClick = { onNavigate(MyCareRoute.Emergency) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("card_emergency")
                    )
                }
            }

            // Dashboard Real-Data Overview Sections
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.dash_overview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Upcoming Reminders & Appointments (All Scheduled Modules)
            item {
                OverviewSectionCard(
                    title = stringResource(R.string.dash_upcoming_reminders),
                    icon = Icons.Default.NotificationsActive,
                    onClick = { onNavigate(MyCareRoute.Tasks) }
                ) {
                    if (overview.upcomingReminders.isEmpty()) {
                        Text(
                            stringResource(R.string.dash_no_reminders),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        overview.upcomingReminders.forEach { r ->
                            Text(
                                "• ${r.title} (${r.featureType.lowercase().replaceFirstChar { it.uppercase() }}) — ${formatDateTimeShort(r.triggerTimeMillis)}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                OverviewSectionCard(
                    title = stringResource(R.string.dash_monthly_wallet),
                    onClick = { onNavigate(MyCareRoute.Wallet) }
                ) {
                    val savings = overview.monthlyIncome - overview.monthlyExpense
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.wallet_total_income),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                String.format("%.2f", overview.monthlyIncome),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                stringResource(R.string.wallet_total_expense),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                String.format("%.2f", overview.monthlyExpense),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                stringResource(R.string.wallet_monthly_savings),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                String.format("%.2f", savings),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                OverviewSectionCard(
                    title = stringResource(R.string.dash_upcoming_medicine),
                    onClick = { onNavigate(MyCareRoute.MedicineList) }
                ) {
                    if (overview.upcomingSchedules.isEmpty()) {
                        Text(
                            stringResource(R.string.dash_no_upcoming_medicine),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        overview.upcomingSchedules.forEach { (medName, sched) ->
                            Text(
                                "• $medName — ${formatTime12h(sched.hour, sched.minute)}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                OverviewSectionCard(
                    title = stringResource(R.string.dash_upcoming_tasks),
                    onClick = { onNavigate(MyCareRoute.Tasks) }
                ) {
                    if (overview.upcomingTasks.isEmpty()) {
                        Text(
                            stringResource(R.string.dash_no_upcoming_tasks),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        overview.upcomingTasks.forEach { task ->
                            Text(
                                "• ${task.title} (${task.priority})",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                OverviewSectionCard(
                    title = stringResource(R.string.dash_upcoming_exercise),
                    onClick = { onNavigate(MyCareRoute.ExerciseList) }
                ) {
                    if (overview.upcomingExercises.isEmpty()) {
                        Text(
                            stringResource(R.string.dash_no_upcoming_exercise),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        overview.upcomingExercises.forEach { ex ->
                            Text(
                                "• ${ex.name} (${ex.sets}×${ex.repetitions}, ${ex.durationMinutes}m)",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                OverviewSectionCard(
                    title = stringResource(R.string.dash_pending_grocery),
                    onClick = { onNavigate(MyCareRoute.Grocery) }
                ) {
                    if (overview.pendingGroceryItems.isEmpty()) {
                        Text(
                            stringResource(R.string.dash_no_pending_grocery),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        overview.pendingGroceryItems.forEach { item ->
                            Text(
                                "• ${item.name} (${item.quantity} ${item.unit})",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                OverviewSectionCard(
                    title = stringResource(R.string.dash_recent_medical),
                    onClick = { onNavigate(MyCareRoute.MedicalRecords) }
                ) {
                    if (overview.recentMedicalFolders.isEmpty()) {
                        Text(
                            stringResource(R.string.dash_no_medical_records),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        overview.recentMedicalFolders.forEach { folder ->
                            Text(
                                "• ${folder.conditionName}" +
                                    if (folder.doctorName.isNotBlank()) " — ${folder.doctorName}" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                OverviewSectionCard(
                    title = stringResource(R.string.dash_upcoming_nutrition),
                    onClick = { onNavigate(MyCareRoute.Nutrition) }
                ) {
                    if (overview.upcomingNutrition.isEmpty()) {
                        Text(
                            stringResource(R.string.dash_no_nutrition),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        overview.upcomingNutrition.forEach { entry ->
                            Text(
                                "• ${entry.foodName} (${entry.mealCategory}) — ${formatTime12h(entry.mealHour, entry.mealMinute)}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun DashboardGridFeatureCard(
    title: String,
    subtitle: String,
    isRealSummary: Boolean,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.heightIn(min = 128.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isRealSummary) FontWeight.Medium else FontWeight.Normal,
                    color = if (isRealSummary) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun OverviewSectionCard(
    title: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            content()
        }
    }
}
