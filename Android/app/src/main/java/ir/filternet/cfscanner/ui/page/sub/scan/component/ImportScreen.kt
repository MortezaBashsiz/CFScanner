package ir.filternet.cfscanner.ui.page.sub.scan.component

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.common.util.concurrent.ListenableFuture
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.utils.BarCodeAnalyser
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun ImportScreen(modifier: Modifier = Modifier , isFirstTime: Boolean = false, scanned: (String) -> Unit = {}) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

        if (isFirstTime){
            Text(text = stringResource(R.string.first_enter_a_config))
        }else{
            Text(text = stringResource(R.string.import_a_new_config))
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Spacer(modifier = Modifier.fillMaxWidth(0.05f))
            QrImportOption(scanned)
            ClipboardImportOption(scanned)
            Spacer(modifier = Modifier.fillMaxWidth(0.05f))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(id = R.string.import_desc), modifier = Modifier.alpha(0.7f),
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(color = Color.Gray, fontSize = 12.sp)
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrImportOption(scanned: (String) -> Unit = {}) {
    var showScanner by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA) {
        if (it) {
            showScanner = !showScanner
        }
    }
    Card(
        Modifier
            .size(100.dp)
            .aspectRatio(1f)
            .clickable { /*qrClicked()*/
                cameraPermissionState.launchPermissionRequest()
            },
        backgroundColor = MaterialTheme.colors.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    Icons.Filled.QrCode, contentDescription = "",
                    Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f)
                )
                Text(text = "QR")
            }
            CameraPreview(showScanner) {
                scanned(it)
                showScanner = false
            }
        }

    }
}

@Composable
fun ClipboardImportOption(scanned: (String) -> Unit = {}) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    Card(
        Modifier
            .size(100.dp)
            .aspectRatio(1f)
            .clickable {
                val text = clipboardManager
                    .getText()
                    .toString()
                if (text.isEmpty()) {
                    Toast
                        .makeText(context, context.getString(R.string.no_qr_found), Toast.LENGTH_SHORT)
                        .show()
                } else {
                    scanned(text)
                }
            },
        backgroundColor = MaterialTheme.colors.primary
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.ContentPaste, contentDescription = "",
                Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
            )
            Text(text = stringResource(R.string.clipboard))
        }
    }
}


@Composable
private fun CameraPreview(show: Boolean = false, scanned: (String) -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var preview by remember { mutableStateOf<Preview?>(null) }
    val barCodeVal = remember { mutableStateOf("") }
    val cameraExecutor: ExecutorService = remember(show) { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { AndroidViewContext ->
            PreviewView(AndroidViewContext).apply {
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (show) 1f else 0f),
        update = { previewView ->
            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val barcodeAnalyser = BarCodeAnalyser { barcodes ->
                    barcodes.forEach { barcode ->
                        barcode.rawValue?.let { barcodeValue ->
                            barCodeVal.value = barcodeValue
                            if (barcodeValue.isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.no_qr_found), Toast.LENGTH_SHORT).show()
                            } else {
                                scanned(barcodeValue)
                            }
                        }
                    }
                }
                val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, barcodeAnalyser)
                    }

                try {
                    cameraProvider.unbindAll()
                    if (show) {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    }
                } catch (e: Exception) {
                    Log.d("TAG", "CameraPreview: ${e.localizedMessage}")
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )

    LaunchedEffect(show) {
        if (!show) {
            cameraExecutor.shutdown()
        }
    }

}