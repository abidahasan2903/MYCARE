package com.example.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.core.di.AppContainer
import com.example.core.media.MediaViewerScreen
import com.example.feature.dashboard.DashboardScreen
import com.example.feature.dashboard.DashboardViewModel
import com.example.feature.doctorsearch.DoctorSearchScreen
import com.example.feature.emergency.EmergencyScreen
import com.example.feature.emergency.EmergencyViewModel
import com.example.feature.exercise.ExerciseDetailScreen
import com.example.feature.exercise.ExerciseListScreen
import com.example.feature.exercise.ExerciseViewModel
import com.example.feature.grocery.GroceryListDetailScreen
import com.example.feature.grocery.GroceryListsScreen
import com.example.feature.grocery.GroceryViewModel
import com.example.feature.healthhub.HealthHubScreen
import com.example.feature.medicalrecords.MedicalFolderDetailScreen
import com.example.feature.medicalrecords.MedicalRecordsScreen
import com.example.feature.medicalrecords.MedicalRecordsViewModel
import com.example.feature.medicine.MedicineDetailScreen
import com.example.feature.medicine.MedicineListScreen
import com.example.feature.medicine.MedicineViewModel
import com.example.feature.memory.AlbumDetailScreen
import com.example.feature.memory.MemoryScreen
import com.example.feature.memory.MemoryViewModel
import com.example.feature.nutrition.NutritionScreen
import com.example.feature.nutrition.NutritionViewModel
import com.example.feature.onboarding.LanguageSelectionScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel
import com.example.feature.tasks.TasksScreen
import com.example.feature.tasks.TasksViewModel
import com.example.feature.travel.TravelDetailScreen
import com.example.feature.travel.TravelListScreen
import com.example.feature.travel.TravelViewModel
import com.example.feature.wallet.WalletScreen
import com.example.feature.wallet.WalletViewModel
import kotlinx.coroutines.launch

private data class BottomTabSpec(
    val route: MyCareRoute,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
private inline fun <reified VM : ViewModel> provideViewModel(
    crossinline creator: () -> VM
): VM {
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
        }
    )
}

@Composable
fun MyCareNavHost(
    container: AppContainer,
    isLanguageSelected: Boolean
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val bottomTabs = listOf(
        BottomTabSpec(MyCareRoute.Home, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        BottomTabSpec(MyCareRoute.Wallet, R.string.nav_wallet, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_wallet"),
        BottomTabSpec(MyCareRoute.HealthHub, R.string.nav_health, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "nav_health"),
        BottomTabSpec(MyCareRoute.Memory, R.string.nav_memory, Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks, "nav_memory"),
        BottomTabSpec(MyCareRoute.Settings, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    val showNavigationChrome = bottomTabs.any { tab ->
        currentDest?.hasRoute(tab.route::class) == true
    }

    val initialDestination = androidx.compose.runtime.remember {
        if (isLanguageSelected) MyCareRoute.Home else MyCareRoute.LanguageSelection
    }

    fun navigateToBottomTab(tabRoute: MyCareRoute) {
        navController.navigate(tabRoute) {
            popUpTo(MyCareRoute.Home) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showNavigationChrome && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToBottomTab(tab.route) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                if (showNavigationChrome && useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToBottomTab(tab.route) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    MyCareNavGraph(
                        navController = navController,
                        container = container,
                        startDestination = initialDestination,
                        onCompleteLanguage = { langCode ->
                            scope.launch {
                                container.settingsStore.completeLanguageSelection(langCode)
                                navController.navigate(MyCareRoute.Home) {
                                    popUpTo(MyCareRoute.LanguageSelection) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyCareNavGraph(
    navController: NavHostController,
    container: AppContainer,
    startDestination: MyCareRoute,
    onCompleteLanguage: (String) -> Unit
) {
    val openMedia: (String, String, String, String) -> Unit = { title, path, mediaType, mimeType ->
        navController.navigate(MyCareRoute.MediaViewer(title, path, mediaType, mimeType))
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<MyCareRoute.LanguageSelection> {
            LanguageSelectionScreen(onContinue = onCompleteLanguage)
        }

        composable<MyCareRoute.Home> {
            val vm = provideViewModel {
                DashboardViewModel(
                    medicineRepo = container.medicineRepository,
                    exerciseRepo = container.exerciseRepository,
                    taskRepo = container.taskRepository,
                    walletRepo = container.walletRepository,
                    groceryRepo = container.groceryRepository,
                    medicalRepo = container.medicalRecordsRepository,
                    nutritionRepo = container.nutritionRepository,
                    reminderRepo = container.reminderRepository
                )
            }
            DashboardScreen(
                viewModel = vm,
                onNavigate = { route ->
                    if (route == MyCareRoute.Wallet) {
                        navController.navigate(MyCareRoute.Wallet) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navController.navigate(route)
                    }
                }
            )
        }

        composable<MyCareRoute.Wallet> {
            val vm = provideViewModel { WalletViewModel(container.walletRepository) }
            WalletScreen(viewModel = vm)
        }

        composable<MyCareRoute.HealthHub> {
            HealthHubScreen(onNavigate = { navController.navigate(it) })
        }

        composable<MyCareRoute.Memory> {
            val vm = provideViewModel {
                MemoryViewModel(
                    memoryRepo = container.memoryRepository,
                    fileStorage = container.fileStorage,
                    audioRecorder = container.audioRecorder,
                    audioPlayer = container.audioPlayer
                )
            }
            MemoryScreen(
                viewModel = vm,
                onOpenAlbum = { albumId -> navController.navigate(MyCareRoute.AlbumDetail(albumId)) }
            )
        }

        composable<MyCareRoute.Settings> {
            val vm = provideViewModel {
                SettingsViewModel(
                    settingsStore = container.settingsStore,
                    fileStorage = container.fileStorage,
                    backupManager = container.backupManager,
                    audioPlayer = container.audioPlayer
                )
            }
            SettingsScreen(viewModel = vm)
        }

        composable<MyCareRoute.MedicineList> {
            val vm = provideViewModel {
                MedicineViewModel(
                    medicineRepo = container.medicineRepository,
                    reminderRepo = container.reminderRepository,
                    fileStorage = container.fileStorage,
                    settingsStore = container.settingsStore
                )
            }
            MedicineListScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenDetail = { medId -> navController.navigate(MyCareRoute.MedicineDetail(medId)) }
            )
        }

        composable<MyCareRoute.MedicineDetail> { entry ->
            val args = entry.toRoute<MyCareRoute.MedicineDetail>()
            val vm = provideViewModel {
                MedicineViewModel(
                    medicineRepo = container.medicineRepository,
                    reminderRepo = container.reminderRepository,
                    fileStorage = container.fileStorage,
                    settingsStore = container.settingsStore
                )
            }
            MedicineDetailScreen(
                medicineId = args.medicineId,
                viewModel = vm,
                audioPlayer = container.audioPlayer,
                onBack = { navController.popBackStack() },
                onOpenMedia = openMedia
            )
        }

        composable<MyCareRoute.ExerciseList> {
            val vm = provideViewModel {
                ExerciseViewModel(
                    exerciseRepo = container.exerciseRepository,
                    reminderRepo = container.reminderRepository,
                    fileStorage = container.fileStorage,
                    settingsStore = container.settingsStore
                )
            }
            ExerciseListScreen(
                viewModel = vm,
                audioPlayer = container.audioPlayer,
                onBack = { navController.popBackStack() },
                onOpenDetail = { exId -> navController.navigate(MyCareRoute.ExerciseDetail(exId)) }
            )
        }

        composable<MyCareRoute.ExerciseDetail> { entry ->
            val args = entry.toRoute<MyCareRoute.ExerciseDetail>()
            val vm = provideViewModel {
                ExerciseViewModel(
                    exerciseRepo = container.exerciseRepository,
                    reminderRepo = container.reminderRepository,
                    fileStorage = container.fileStorage,
                    settingsStore = container.settingsStore
                )
            }
            ExerciseDetailScreen(
                exerciseId = args.exerciseId,
                viewModel = vm,
                audioPlayer = container.audioPlayer,
                onBack = { navController.popBackStack() },
                onOpenMedia = openMedia
            )
        }

        composable<MyCareRoute.Tasks> {
            val vm = provideViewModel {
                TasksViewModel(
                    taskRepo = container.taskRepository,
                    reminderRepo = container.reminderRepository,
                    fileStorage = container.fileStorage,
                    settingsStore = container.settingsStore
                )
            }
            TasksScreen(
                viewModel = vm,
                audioPlayer = container.audioPlayer,
                onBack = { navController.popBackStack() },
                onOpenMedia = openMedia
            )
        }

        composable<MyCareRoute.Grocery> {
            val vm = provideViewModel {
                GroceryViewModel(
                    groceryRepo = container.groceryRepository,
                    fileStorage = container.fileStorage
                )
            }
            GroceryListsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenList = { listId -> navController.navigate(MyCareRoute.GroceryListDetail(listId)) }
            )
        }

        composable<MyCareRoute.GroceryListDetail> { entry ->
            val args = entry.toRoute<MyCareRoute.GroceryListDetail>()
            val vm = provideViewModel {
                GroceryViewModel(
                    groceryRepo = container.groceryRepository,
                    fileStorage = container.fileStorage
                )
            }
            GroceryListDetailScreen(
                listId = args.listId,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenMedia = openMedia
            )
        }

        composable<MyCareRoute.AlbumDetail> { entry ->
            val args = entry.toRoute<MyCareRoute.AlbumDetail>()
            val vm = provideViewModel {
                MemoryViewModel(
                    memoryRepo = container.memoryRepository,
                    fileStorage = container.fileStorage,
                    audioRecorder = container.audioRecorder,
                    audioPlayer = container.audioPlayer
                )
            }
            AlbumDetailScreen(
                albumId = args.albumId,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenMedia = openMedia
            )
        }

        composable<MyCareRoute.MedicalRecords> {
            val vm = provideViewModel {
                MedicalRecordsViewModel(
                    repo = container.medicalRecordsRepository,
                    fileStorage = container.fileStorage
                )
            }
            MedicalRecordsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenFolder = { folderId -> navController.navigate(MyCareRoute.MedicalFolderDetail(folderId)) },
                onOpenMedia = openMedia
            )
        }

        composable<MyCareRoute.MedicalFolderDetail> { entry ->
            val args = entry.toRoute<MyCareRoute.MedicalFolderDetail>()
            val vm = provideViewModel {
                MedicalRecordsViewModel(
                    repo = container.medicalRecordsRepository,
                    fileStorage = container.fileStorage
                )
            }
            MedicalFolderDetailScreen(
                folderId = args.folderId,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenMedia = openMedia
            )
        }

        composable<MyCareRoute.DoctorSearch> {
            DoctorSearchScreen(onBack = { navController.popBackStack() })
        }

        composable<MyCareRoute.Nutrition> {
            val vm = provideViewModel {
                NutritionViewModel(
                    nutritionRepo = container.nutritionRepository,
                    reminderRepo = container.reminderRepository,
                    fileStorage = container.fileStorage,
                    settingsStore = container.settingsStore
                )
            }
            NutritionScreen(
                viewModel = vm,
                audioPlayer = container.audioPlayer,
                onBack = { navController.popBackStack() },
                onOpenMedia = openMedia
            )
        }

        composable<MyCareRoute.Travel> {
            val vm = provideViewModel {
                TravelViewModel(
                    travelRepo = container.travelRepository,
                    reminderRepo = container.reminderRepository,
                    fileStorage = container.fileStorage
                )
            }
            TravelListScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenDetail = { id -> navController.navigate(MyCareRoute.TravelDetail(id)) }
            )
        }

        composable<MyCareRoute.TravelDetail> { entry ->
            val args = entry.toRoute<MyCareRoute.TravelDetail>()
            val vm = provideViewModel {
                TravelViewModel(
                    travelRepo = container.travelRepository,
                    reminderRepo = container.reminderRepository,
                    fileStorage = container.fileStorage
                )
            }
            TravelDetailScreen(
                travelId = args.travelId,
                viewModel = vm,
                audioPlayer = container.audioPlayer,
                onBack = { navController.popBackStack() },
                onOpenMedia = openMedia
            )
        }

        composable<MyCareRoute.Emergency> {
            val vm = provideViewModel { EmergencyViewModel(container.emergencyRepository) }
            EmergencyScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable<MyCareRoute.MediaViewer> { entry ->
            val args = entry.toRoute<MyCareRoute.MediaViewer>()
            MediaViewerScreen(
                title = args.title,
                filePath = args.filePath,
                mediaType = args.mediaType,
                mimeType = args.mimeType,
                fileStorage = container.fileStorage,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
