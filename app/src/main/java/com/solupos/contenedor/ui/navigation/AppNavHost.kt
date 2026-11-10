package com.solupos.contenedor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.solupos.contenedor.SoluPosApp
import com.solupos.contenedor.ui.pricecheck.PriceCheckScreen
import com.solupos.contenedor.ui.screens.*
import com.solupos.contenedor.viewmodel.*

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as SoluPosApp

    // Equipo configurado como quiosco: al abrir la app va directo al consultador.
    LaunchedEffect(Unit) {
        if (!com.solupos.contenedor.Features.priceCheck) return@LaunchedEffect
        app.storeRepository.getKioskStore()?.let { store ->
            navController.navigate(Screen.PriceCheck.createRoute(store.id))
        }
    }

    NavHost(navController = navController, startDestination = Screen.StoreList.route) {

        composable(Screen.StoreList.route) {
            val viewModel: StoreListViewModel = viewModel(
                factory = StoreListViewModelFactory(app.storeRepository, app.userPreferences)
            )
            StoreListScreen(
                viewModel = viewModel,
                onAddStore = { navController.navigate(Screen.StoreForm.createRoute()) },
                onEditStore = { id -> navController.navigate(Screen.StoreForm.createRoute(id)) },
                onOpenStore = { id -> navController.navigate(Screen.WebView.createRoute(id)) },
                onOpenPriceCheck = { id -> navController.navigate(Screen.PriceCheck.createRoute(id)) },
                hasToken = { id -> app.tokenStore.get(id) != null },
                onOpenPrinterSettings = { navController.navigate(Screen.PrinterSettings.route) }
            )
        }

        composable(
            route = Screen.StoreForm.route,
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId")?.takeIf { it != "new" }
            val viewModel: StoreFormViewModel = viewModel(
                factory = StoreFormViewModelFactory(app.storeRepository, storeId, app.tokenStore, app.kioskPinStore)
            )
            StoreFormScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.WebView.route,
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: return@composable
            WebViewScreen(
                storeId = storeId,
                repository = app.storeRepository,
                userPreferences = app.userPreferences,
                printerManager = app.printerManager,
                priceLookup = app.priceLookup,
                onBack = { navController.popBackStack() },
                onOpenPrinterSettings = { navController.navigate(Screen.PrinterSettings.route) }
            )
        }

        composable(
            route = Screen.PriceCheck.route,
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: return@composable
            PriceCheckScreen(
                storeId = storeId,
                repository = app.storeRepository,
                lookup = app.priceLookup,
                pinStore = app.kioskPinStore,
                onExit = { navController.popBackStack() }
            )
        }

        composable(Screen.PrinterSettings.route) {
            val viewModel: PrinterSettingsViewModel = viewModel(
                factory = PrinterSettingsViewModelFactory(app, app.userPreferences, app.printerManager)
            )
            PrinterSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
