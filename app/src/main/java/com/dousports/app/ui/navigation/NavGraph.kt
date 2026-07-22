package com.dousports.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.dousports.app.ui.screens.exercises.ExerciseDetailScreen
import com.dousports.app.ui.screens.exercises.ExercisesScreen
import com.dousports.app.ui.screens.history.WorkoutHistoryScreen
import com.dousports.app.ui.screens.home.HomeScreen
import com.dousports.app.ui.screens.profile.ProfileScreen
import com.dousports.app.ui.screens.exercises.CreateExerciseScreen
import com.dousports.app.ui.screens.qr.QrScannerScreen
import com.dousports.app.ui.screens.routines.CreateRoutineScreen
import com.dousports.app.ui.screens.routines.RoutinesScreen
import com.dousports.app.ui.screens.schedule.WeeklyScheduleScreen
import com.dousports.app.ui.screens.stats.StatsScreen
import com.dousports.app.ui.screens.update.UpdateScreen
import com.dousports.app.ui.screens.workout.ActiveWorkoutScreen
import com.dousports.app.ui.screens.workout.TimedWorkoutScreen
import com.dousports.app.ui.viewmodel.UpdateCheckViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Exercises : Screen("exercises")
    object ExerciseDetail : Screen("exercise/{exerciseId}") {
        fun createRoute(exerciseId: String) = "exercise/$exerciseId"
    }
    object Routines : Screen("routines")
    object CreateRoutine : Screen("create-routine?routineId={routineId}") {
        fun createRoute(routineId: Long? = null) =
            if (routineId != null) "create-routine?routineId=$routineId" else "create-routine"
    }
    object ActiveWorkout : Screen("active-workout/{routineId}") {
        fun createRoute(routineId: Long) = "active-workout/$routineId"
    }
    object TimedWorkout : Screen("timed-workout/{routineId}") {
        fun createRoute(routineId: Long) = "timed-workout/$routineId"
    }
    object CreateExercise : Screen("create-exercise?exerciseId={exerciseId}") {
        fun createRoute(exerciseId: String? = null) =
            if (exerciseId != null) "create-exercise?exerciseId=$exerciseId" else "create-exercise"
    }
    object Stats : Screen("stats")
    object Profile : Screen("profile")
    object WorkoutHistory : Screen("workout-history")
    object WeeklySchedule : Screen("weekly-schedule")
    object Update : Screen("update")
    object QrScanner : Screen("qr-scanner")
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Accueil", Icons.Default.Home, Screen.Home.route),
    BottomNavItem("Exercices", Icons.Default.FitnessCenter, Screen.Exercises.route),
    BottomNavItem("Routines", Icons.Default.ListAlt, Screen.Routines.route),
    BottomNavItem("Profil", Icons.Default.Person, Screen.Profile.route)
)

@Composable
fun DouSportsNavGraph(onNavControllerReady: (NavController) -> Unit = {}) {
    val navController = rememberNavController()

    LaunchedEffect(navController) { onNavControllerReady(navController) }
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    val updateVm: UpdateCheckViewModel = hiltViewModel()
    val updateInfo by updateVm.updateInfo.collectAsState()
    val showUpdateDialog by updateVm.showUpdateDialog.collectAsState()

    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { updateVm.dismissDialog() },
            title = { Text("Mise à jour disponible") },
            text = { Text("La version ${updateInfo!!.latestVersion} est disponible.") },
            confirmButton = {
                TextButton(onClick = {
                    updateVm.dismissDialog()
                    navController.navigate(Screen.Update.route) { launchSingleTop = true }
                }) { Text("Voir la mise à jour") }
            },
            dismissButton = {
                TextButton(onClick = { updateVm.dismissDialog() }) { Text("Plus tard") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onStartWorkout = { routineId, isTimed ->
                        if (isTimed) {
                            navController.navigate(Screen.TimedWorkout.createRoute(routineId))
                        } else {
                            navController.navigate(Screen.ActiveWorkout.createRoute(routineId))
                        }
                    },
                    onNavigateToRoutines = {
                        navController.navigate(Screen.Routines.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Exercises.route) {
                ExercisesScreen(
                    onExerciseClick = { exerciseId ->
                        navController.navigate(Screen.ExerciseDetail.createRoute(exerciseId))
                    },
                    onCreateExercise = {
                        navController.navigate(Screen.CreateExercise.createRoute())
                    }
                )
            }

            composable(
                route = Screen.ExerciseDetail.route,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
            ) { backStack ->
                val exerciseId = backStack.arguments?.getString("exerciseId") ?: return@composable
                ExerciseDetailScreen(
                    exerciseId = exerciseId,
                    onBack = { navController.popBackStack() },
                    onEditExercise = { id ->
                        navController.navigate(Screen.CreateExercise.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.CreateExercise.route,
                arguments = listOf(
                    navArgument("exerciseId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStack ->
                val exerciseId = backStack.arguments?.getString("exerciseId")
                CreateExerciseScreen(
                    exerciseId = exerciseId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Routines.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "dousports://import?code={code}" }
                ),
                arguments = listOf(
                    navArgument("code") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStack ->
                val deepLinkCode = backStack.arguments?.getString("code")
                val scannedCode by backStack.savedStateHandle
                    .getStateFlow("scanned_code", null as String?)
                    .collectAsState()
                val autoImportCode = deepLinkCode ?: scannedCode
                RoutinesScreen(
                    autoImportCode = autoImportCode,
                    onAutoImportConsumed = {
                        backStack.savedStateHandle.remove<String>("scanned_code")
                    },
                    onCreateRoutine = {
                        navController.navigate(Screen.CreateRoutine.createRoute())
                    },
                    onEditRoutine = { routineId ->
                        navController.navigate(Screen.CreateRoutine.createRoute(routineId))
                    },
                    onStartRoutine = { routineId, isTimed ->
                        if (isTimed) {
                            navController.navigate(Screen.TimedWorkout.createRoute(routineId))
                        } else {
                            navController.navigate(Screen.ActiveWorkout.createRoute(routineId))
                        }
                    },
                    onNavigateToSchedule = {
                        navController.navigate(Screen.WeeklySchedule.route)
                    },
                    onOpenQrScanner = {
                        navController.navigate(Screen.QrScanner.route)
                    }
                )
            }

            composable(Screen.QrScanner.route) {
                QrScannerScreen(
                    onCodeDetected = { code ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scanned_code", code)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.WeeklySchedule.route) {
                WeeklyScheduleScreen(
                    onBack = { navController.popBackStack() },
                    onStartRoutine = { routineId, isTimed ->
                        if (isTimed) {
                            navController.navigate(Screen.TimedWorkout.createRoute(routineId))
                        } else {
                            navController.navigate(Screen.ActiveWorkout.createRoute(routineId))
                        }
                    }
                )
            }

            composable(
                route = Screen.CreateRoutine.route,
                arguments = listOf(
                    navArgument("routineId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStack ->
                val routineId = backStack.arguments?.getLong("routineId")?.takeIf { it != -1L }
                CreateRoutineScreen(
                    routineId = routineId,
                    onBack = { navController.popBackStack() },
                    onExercisePicker = { exerciseId ->
                        navController.navigate(Screen.ExerciseDetail.createRoute(exerciseId))
                    }
                )
            }

            composable(
                route = Screen.ActiveWorkout.route,
                arguments = listOf(navArgument("routineId") { type = NavType.LongType })
            ) { backStack ->
                val routineId = backStack.arguments?.getLong("routineId") ?: return@composable
                ActiveWorkoutScreen(
                    routineId = routineId,
                    onFinish = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TimedWorkout.route,
                arguments = listOf(navArgument("routineId") { type = NavType.LongType })
            ) { backStack ->
                val routineId = backStack.arguments?.getLong("routineId") ?: return@composable
                TimedWorkoutScreen(
                    routineId = routineId,
                    onFinish = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                    onNavigateToHistory = { navController.navigate(Screen.WorkoutHistory.route) }
                )
            }

            composable(Screen.WorkoutHistory.route) {
                WorkoutHistoryScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Update.route) {
                UpdateScreen(
                    viewModel = updateVm,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
