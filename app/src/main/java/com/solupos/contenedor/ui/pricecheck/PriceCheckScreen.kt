package com.solupos.contenedor.ui.pricecheck

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.solupos.contenedor.data.db.StoreEntity
import com.solupos.contenedor.data.repository.StoreRepository
import com.solupos.contenedor.pricecheck.CodeInterpreter
import com.solupos.contenedor.pricecheck.CodeQuery
import com.solupos.contenedor.pricecheck.LookupResult
import com.solupos.contenedor.pricecheck.GuatemalaZone
import com.solupos.contenedor.pricecheck.PriceLookupService
import com.solupos.contenedor.pricecheck.formatQuetzales
import com.solupos.contenedor.pricecheck.message
import com.solupos.contenedor.security.KioskPinStore
import com.solupos.contenedor.security.PinCheck
import com.solupos.contenedor.ui.theme.Blue100
import com.solupos.contenedor.ui.theme.HeroGradientBottom
import com.solupos.contenedor.ui.theme.HeroGradientTop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val RESULT_SECONDS = 8
private const val ERROR_SECONDS = 5
private const val EXIT_HOLD_MS = 3_000L

private sealed interface KioskState {
    data object Idle : KioskState
    data object Looking : KioskState
    data class Showing(val result: LookupResult) : KioskState
}

/**
 * Consultador a pantalla completa. Si la tienda está en modo quiosco el equipo
 * queda bloqueado aquí y solo se sale con el PIN; si no, es una vista normal
 * con botón para volver.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PriceCheckScreen(
    storeId: String,
    repository: StoreRepository,
    lookup: PriceLookupService,
    pinStore: KioskPinStore,
    onExit: () -> Unit
) {
    var store by remember { mutableStateOf<StoreEntity?>(null) }
    LaunchedEffect(storeId) { store = repository.getById(storeId) }
    val locked = store?.kioskMode == true

    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<KioskState>(KioskState.Idle) }
    var showPin by remember { mutableStateOf(false) }
    val permission = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) { if (!permission.status.isGranted) permission.launchPermissionRequest() }

    // Pantalla siempre encendida mientras el consultador está abierto.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    // Fijado de pantalla: el cliente no puede salir a otras apps.
    DisposableEffect(locked) {
        val activity = context as? Activity
        if (locked) runCatching { activity?.startLockTask() }
        onDispose { if (locked) runCatching { activity?.stopLockTask() } }
    }

    BackHandler { if (locked) showPin = true else onExit() }

    // Vuelve solo a "Escanee su producto" tras unos segundos.
    val progress = remember { Animatable(1f) }
    LaunchedEffect(state) {
        val s = state
        if (s is KioskState.Showing) {
            val seconds = if (s.result is LookupResult.Found) RESULT_SECONDS else ERROR_SECONDS
            progress.snapTo(1f)
            progress.animateTo(0f, tween(seconds * 1000, easing = LinearEasing))
            state = KioskState.Idle
        }
    }

    fun onCode(raw: String) {
        if (state !is KioskState.Idle) return
        state = KioskState.Looking
        scope.launch {
            val query = CodeInterpreter.interpret(raw)
            state = KioskState.Showing(
                if (query is CodeQuery.Unrecognized) LookupResult.Unrecognized
                else lookup.lookup(storeId, query)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HeroGradientTop, HeroGradientBottom)))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                (store?.name ?: "").uppercase(),
                color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, letterSpacing = 2.sp
            )
            Text(
                "Escanee su producto",
                color = Color.White, fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 16.dp)
            )
            if (permission.status.isGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                ) {
                    ContinuousScanner(
                        modifier = Modifier.fillMaxSize(),
                        useFrontCamera = store?.kioskFrontCamera == true,
                        active = state is KioskState.Idle,
                        onCode = ::onCode
                    )
                }
                Text(
                    "Acerque el código de barras a la cámara",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                Text(
                    "Se necesita permiso de cámara para escanear.",
                    color = Color.White, textAlign = TextAlign.Center
                )
                Button(onClick = { permission.launchPermissionRequest() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Permitir cámara")
                }
            }
        }

        when (val s = state) {
            KioskState.Looking -> Box(
                Modifier.fillMaxSize().background(Color(0xCC041B4D)),
                contentAlignment = Alignment.Center
            ) { Text("Consultando…", color = Color.White, fontSize = 24.sp) }

            is KioskState.Showing -> ResultOverlay(s.result, progress.value) { state = KioskState.Idle }
            KioskState.Idle -> Unit
        }

        // Salida: mantener pulsada la esquina (con PIN) o botón normal si no es quiosco.
        if (locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(72.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            val job = scope.launch { delay(EXIT_HOLD_MS); showPin = true }
                            tryAwaitRelease()
                            job.cancel()
                        })
                    }
            )
        } else {
            TextButton(onClick = onExit, modifier = Modifier.align(Alignment.TopStart)) {
                Text("Salir", color = Color.White)
            }
        }
    }

    if (showPin) {
        PinDialog(pinStore = pinStore, onDismiss = { showPin = false }, onOk = { showPin = false; onExit() })
    }
}

@Composable
private fun ResultOverlay(result: LookupResult, progress: Float, onDismiss: () -> Unit) {
    val today = LocalDate.now(GuatemalaZone)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HeroGradientTop, HeroGradientBottom)))
            .clickable(onClick = onDismiss)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (result is LookupResult.Found) {
                val info = result.info
                val promo = info.activePromo(today)
                Text(info.displayName, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center)
                Text(
                    formatQuetzales(info.finalPrice(today)),
                    color = Color.White, fontSize = 88.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                if (promo != null) {
                    Text(
                        "Antes ${formatQuetzales(info.regularPrice)}" +
                            (info.promoEnd?.let { " · Oferta hasta ${it.dayOfMonth}/${it.monthValue}" } ?: ""),
                        color = Blue100, fontSize = 20.sp
                    )
                }
                when (info.available) {
                    true -> Text("Disponible", color = Color(0xFF7DE0A8), fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp))
                    false -> Text("Agotado", color = Color(0xFFFFB4AB), fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp))
                    null -> Unit
                }
            } else {
                val msg = result.message(kiosk = true)
                Text(msg?.title ?: "", color = Color.White, fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center)
                msg?.detail?.let {
                    Text(it, color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(6.dp),
            color = Blue100, trackColor = Color.Transparent
        )
    }
}

@Composable
private fun PinDialog(pinStore: KioskPinStore, onDismiss: () -> Unit, onOk: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var message by remember {
        val locked = pinStore.lockedSeconds()
        mutableStateOf(if (locked > 0) "Demasiados intentos. Espere $locked s." else null)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN del encargado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8); message = null },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                message?.let { Text(it, color = Color(0xFFB3261E)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (val r = pinStore.verify(pin)) {
                    PinCheck.Ok -> onOk()
                    is PinCheck.Wrong -> { message = "PIN incorrecto. Intentos restantes: ${r.attemptsBeforeLock}"; pin = "" }
                    is PinCheck.Locked -> { message = "Demasiados intentos. Espere ${r.seconds} s."; pin = "" }
                }
            }) { Text("Salir del quiosco") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
