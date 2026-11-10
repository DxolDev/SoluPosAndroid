package com.solupos.contenedor.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solupos.contenedor.R
import com.solupos.contenedor.data.db.StoreEntity
import com.solupos.contenedor.ui.components.StoreCard
import com.solupos.contenedor.ui.components.TutorialOverlay
import com.solupos.contenedor.ui.components.TutorialStep
import com.solupos.contenedor.ui.theme.HeroGradientBottom
import com.solupos.contenedor.ui.theme.HeroGradientTop
import com.solupos.contenedor.viewmodel.StoreListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreListScreen(
    viewModel: StoreListViewModel,
    onAddStore: () -> Unit,
    onEditStore: (String) -> Unit,
    onOpenStore: (String) -> Unit,
    onOpenPriceCheck: (String) -> Unit,
    hasToken: (String) -> Boolean,
    onOpenPrinterSettings: () -> Unit
) {
    val stores by viewModel.stores.collectAsState()
    val showTutorial by viewModel.showTutorial.collectAsState()
    val showPriceTip by viewModel.showPriceTip.collectAsState()
    var storeToDelete by remember { mutableStateOf<StoreEntity?>(null) }
    var printerIconBounds by remember { mutableStateOf<Rect?>(null) }
    var addFabBounds by remember { mutableStateOf<Rect?>(null) }
    var priceBtnBounds by remember { mutableStateOf<Rect?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mis Tiendas") },
                    actions = {
                        IconButton(onClick = { viewModel.replayTutorial() }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Ayuda")
                        }
                        IconButton(
                            onClick = onOpenPrinterSettings,
                            modifier = Modifier.onGloballyPositioned { printerIconBounds = it.boundsInRoot() }
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Configurar impresora")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HeroGradientTop,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddStore,
                    modifier = Modifier.onGloballyPositioned { addFabBounds = it.boundsInRoot() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar tienda")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(HeroGradientTop, HeroGradientBottom)))
                        .padding(vertical = 28.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_solupos),
                        contentDescription = "SoluPos",
                        modifier = Modifier.height(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "La solución completa para tu punto de venta",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                if (stores.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay tiendas guardadas.\nToca + para agregar una.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(stores, key = { _, s -> s.id }) { index, store ->
                            StoreCard(
                                store = store,
                                onClick = {
                                    viewModel.saveLastStore(store.id)
                                    onOpenStore(store.id)
                                },
                                onEdit = { onEditStore(store.id) },
                                onDelete = { storeToDelete = store },
                                onPriceCheck = { onOpenPriceCheck(store.id) },
                                // En desarrollo se habilita siempre para ver el catálogo de demostración.
                                priceCheckEnabled = com.solupos.contenedor.BuildConfig.DEBUG ||
                                    remember(store.id) { hasToken(store.id) },
                                priceCheckButtonModifier = if (index == 0) {
                                    Modifier.onGloballyPositioned { priceBtnBounds = it.boundsInRoot() }
                                } else Modifier
                            )
                        }
                    }
                }
            }
        }

        storeToDelete?.let { store ->
            AlertDialog(
                onDismissRequest = { storeToDelete = null },
                title = { Text("Eliminar tienda") },
                text = { Text("¿Eliminar \"${store.name}\"?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteStore(store); storeToDelete = null }) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { storeToDelete = null }) { Text("Cancelar") }
                }
            )
        }

        val printerBounds = printerIconBounds
        val fabBounds = addFabBounds
        val priceBounds = priceBtnBounds
        val tutorialSteps = buildList {
            if (showTutorial && printerBounds != null && fabBounds != null) {
                add(
                    TutorialStep(
                        targetBounds = printerBounds,
                        title = "Configura tu impresora",
                        description = "Aquí puedes vincular tu impresora térmica Bluetooth para imprimir tickets de venta."
                    )
                )
                add(
                    TutorialStep(
                        targetBounds = fabBounds,
                        title = "Agrega tu primera tienda",
                        description = "Toca este botón para conectar tu punto de venta y empezar a vender."
                    )
                )
            }
            // Novedad: se muestra una vez, incluso a quien ya vio el tutorial original.
            if (com.solupos.contenedor.Features.priceCheck && showPriceTip && priceBounds != null && stores.isNotEmpty()) {
                add(
                    TutorialStep(
                        targetBounds = priceBounds,
                        title = "Nuevo: consultador de precios",
                        description = "Escanea un producto con la cámara y mira su precio al instante. " +
                            "En Editar tienda puedes activar el modo quiosco para dejar un equipo fijo en el pasillo."
                    )
                )
            }
        }
        if (tutorialSteps.isNotEmpty()) {
            TutorialOverlay(steps = tutorialSteps, onFinish = { viewModel.dismissTutorial() })
        }
    }
}
