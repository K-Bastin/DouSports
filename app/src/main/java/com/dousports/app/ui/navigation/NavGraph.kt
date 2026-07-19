package com.dousports.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.*
import androidx.navigation.compose.*
import com.dousports.app.ui.screens.calendar.CalendarScreen
import com.dousports.app.ui.screens.exercises.ExerciseDetailScreen
import com.dousports.app.ui.screens.exercises.ExercisesScreen
import com.dousports.app.ui.screens.home.HomeScreen
import com.dousports.app.ui.screens.profile.ProfileScreen
import com.dousports.app.ui.screens.exercises.CreateExerciseScreen
import com.dousports.app.ui.screens.routines.CreateRoutineScreen
import com.dousports.app.ui.screens.routines.RoutinesScreen
import com.dousports.app.ui.screens.stats.StatsScreen
import com.dousports.app.ui.screens.workout.ActiveWorkoutScreen

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
    object CreateExercise : Screen("create-exercise?exerciseId={exerciseId}") {
        fun createRoute(exerciseId: String? = null) =
            if (exerciseId != null) "create-exercise?exerciseId=$exerciseId" else "create-exercise"
    }
    object Stats : Screen("stats")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
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
    BottomNavItem("Calendrier", Icons.Default.CalendarMonth, Screen.Calendar.route),
    BottomNavItem("Profil", Icons.Default.Person, Screen.Profile.route)
)

@Composable
fun DouSportsNavGraph() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
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
                    onStartWorkout = { routineId ->
                        navController.navigate(Screen.ActiveWorkout.createRoute(routineId))
                    },
                    onNavigateToRoutines = {
                        navController.navigate(Screen.Routines.route)
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

            composable(Screen.Routines.route) {
                RoutinesScreen(
                    onCreateRoutine = {
                        navController.navigate(Screen.CreateRoutine.createRoute())
                    },
                    onEditRoutine = { routineId ->
                        navController.navigate(Screen.CreateRoutine.createRoute(routineId))
                    },
                    onStartRoutine = { routineId ->
                        navController.navigate(Screen.ActiveWorkout.createRoute(routineId))
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

            composable(Screen.Stats.route) {
                StatsScreen()
            }

            composable(Screen.Calendar.route) {
                CalendarScreen()
            }

            composable(Screen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
