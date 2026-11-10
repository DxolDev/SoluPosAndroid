package com.solupos.contenedor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.solupos.contenedor.viewmodel.StoreFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreFormScreen(
    viewModel: StoreFormViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Editar Tienda" else "Nueva Tienda") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre de la tienda") },
                placeholder = { Text("Ej: Sucursal Centro") },
                isError = state.nameError,
                supportingText = if (state.nameError) ({ Text("El nombre es requerido") }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = state.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text("URL del sistema POS") },
                placeholder = { Text("http://192.168.1.10/pos") },
                isError = state.urlError,
                supportingText = if (state.urlError) ({ Text("La URL es requerida") }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                )
            )

            if (com.solupos.contenedor.Features.priceCheck) {
            Text("Consultador de precios", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = state.token,
                onValueChange = viewModel::onTokenChange,
                label = { Text("Token de consulta") },
                placeholder = { Text(if (state.hasToken) "Guardado. Deja vacío para conservarlo" else "Lo entrega el administrador") },
                supportingText = { Text("Se guarda cifrado en este equipo.") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Modo quiosco")
                    Text(
                        "Al abrir la app entra directo al consultador de esta tienda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = state.kioskMode, onCheckedChange = viewModel::onKioskModeChange)
            }

            if (state.kioskMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Usar cámara frontal", modifier = Modifier.weight(1f))
                    Switch(checked = state.kioskFrontCamera, onCheckedChange = viewModel::onKioskFrontCameraChange)
                }
                OutlinedTextField(
                    value = state.pin,
                    onValueChange = viewModel::onPinChange,
                    label = { Text("PIN para salir del quiosco") },
                    isError = state.pinError,
                    supportingText = {
                        Text(
                            when {
                                state.pinError -> "El PIN debe tener de 4 a 8 dígitos"
                                state.deviceHasPin -> "Ya hay un PIN en este equipo. Déjalo vacío para conservarlo."
                                else -> "De 4 a 8 dígitos"
                            }
                        )
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done)
                )
            }

            } // Features.priceCheck

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Guardar Tienda")
            }
        }
    }
}
