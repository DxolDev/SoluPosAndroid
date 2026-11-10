package com.solupos.contenedor.ui.pricecheck

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.solupos.contenedor.pricecheck.ScanStabilizer
import java.util.concurrent.Executors

/**
 * Cámara que sigue leyendo códigos sin cerrarse (el escáner del POS es de un
 * solo disparo). Mientras [active] sea false las lecturas se descartan, pero la
 * cámara sigue encendida para no tardar en reiniciarla entre consultas.
 * El permiso de cámara debe estar concedido antes de componerlo.
 */
@Composable
fun ContinuousScanner(
    modifier: Modifier = Modifier,
    useFrontCamera: Boolean = false,
    torchOn: Boolean = false,
    active: Boolean = true,
    onCode: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentActive by rememberUpdatedState(active)
    val currentOnCode by rememberUpdatedState(onCode)
    val stabilizer = remember { ScanStabilizer() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            provider?.unbindAll()
            executor.shutdown()
        }
    }
    DisposableEffect(camera, torchOn) {
        camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(torchOn)
        onDispose { }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val cameraProvider = future.get()
                provider = cameraProvider

                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder().setBarcodeFormats(
                        Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E
                    ).build()
                )
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { a ->
                        a.setAnalyzer(executor) { proxy ->
                            val media = proxy.image
                            if (!currentActive || media == null) {
                                proxy.close()
                                return@setAnalyzer
                            }
                            scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                                .addOnSuccessListener { codes ->
                                    val confirmed = stabilizer.offer(codes.firstNotNullOfOrNull { it.rawValue })
                                    if (confirmed != null && currentActive) currentOnCode(confirmed)
                                }
                                .addOnCompleteListener { proxy.close() }
                        }
                    }

                val wanted = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val selector = if (cameraProvider.hasCamera(wanted)) wanted else CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
