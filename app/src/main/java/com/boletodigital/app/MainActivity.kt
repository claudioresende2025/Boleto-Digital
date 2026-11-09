package com.boletodigital.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.boletodigital.app.data.BoletoDatabase
import com.boletodigital.app.data.BoletoRepository
import com.boletodigital.app.ui.screens.DetailScreen
import com.boletodigital.app.ui.screens.EditScreen
import com.boletodigital.app.ui.screens.HomeScreen
import com.boletodigital.app.ui.theme.BoletoDigitalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = BoletoRepository(BoletoDatabase.get(this).boletoDao())

        setContent {
            BoletoDigitalTheme {
                BoletoDigitalApp(repository)
            }
        }
    }
}

@Composable
private fun BoletoDigitalApp(repository: BoletoRepository) {
    val navController = rememberNavController()
    val viewModel: BoletoViewModel = viewModel(factory = BoletoViewModel.Factory(repository))
    val boletos by viewModel.boletos.collectAsState()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                boletos = boletos,
                onAdd = { navController.navigate("edit/0") },
                onOpen = { id -> navController.navigate("detail/$id") }
            )
        }
        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val boletoFlow = remember(id) { viewModel.observar(id) }
            val boleto by boletoFlow.collectAsState(initial = null)
            val atual = boleto ?: return@composable

            DetailScreen(
                boleto = atual,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("edit/$id") },
                onTogglePago = { pago -> viewModel.marcarPago(atual, pago) },
                onDelete = {
                    viewModel.excluir(atual) {
                        navController.popBackStack("home", inclusive = false)
                    }
                }
            )
        }
        composable(
            route = "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val existente = boletos.firstOrNull { it.id == id }
            EditScreen(
                boleto = existente,
                onBack = { navController.popBackStack() },
                onSave = { savedId, titulo, vencimento, valor, desconto, pix ->
                    viewModel.salvar(savedId, titulo, vencimento, valor, desconto, pix) { novoId ->
                        if (savedId == 0L) {
                            navController.navigate("detail/$novoId") {
                                popUpTo("home")
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }
    }
}
