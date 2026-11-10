package com.solupos.contenedor.ui.pricecheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solupos.contenedor.pricecheck.LookupResult
import com.solupos.contenedor.pricecheck.PriceInfo
import com.solupos.contenedor.pricecheck.formatQuetzales
import com.solupos.contenedor.pricecheck.message
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val GreenOk = Color(0xFF1B7F4B)
private val RedOut = Color(0xFFB3261E)
private val AmberPromo = Color(0xFFA15C00)

/** Resultado de la consulta mostrado sobre la venta; no toca el carrito. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceResultSheet(
    result: LookupResult,
    scannedCode: String,
    today: LocalDate,
    onClose: () -> Unit,
    onScanAnother: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (result is LookupResult.Found) {
                FoundContent(result.info, today)
            } else {
                val msg = result.message(kiosk = false)
                Text(
                    scannedCode, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(msg?.title ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                msg?.detail?.let {
                    Text(
                        it, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Cerrar") }
                Button(onClick = onScanAnother, modifier = Modifier.weight(1f)) { Text("Escanear otro") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FoundContent(info: PriceInfo, today: LocalDate) {
    val promo = info.activePromo(today)
    Text(
        info.itemNumber ?: info.itemId.toString(), fontFamily = FontFamily.Monospace, fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(info.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        formatQuetzales(info.finalPrice(today)),
        fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
    )
    if (promo != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                formatQuetzales(info.regularPrice), textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                info.promoEnd?.let { "Oferta hasta ${it.format(DateTimeFormatter.ofPattern("dd/MM"))}" } ?: "Oferta",
                color = AmberPromo, fontWeight = FontWeight.SemiBold
            )
        }
    }
    when (info.available) {
        true -> Text("Disponible", color = GreenOk, fontWeight = FontWeight.SemiBold)
        false -> Text("Agotado", color = RedOut, fontWeight = FontWeight.SemiBold)
        null -> Unit
    }
}
