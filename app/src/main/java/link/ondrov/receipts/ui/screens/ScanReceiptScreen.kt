package link.ondrov.receipts.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import link.ondrov.receipts.ui.components.ErrorRetryDialog
import link.ondrov.receipts.ui.components.ExpressiveLoadingState
import link.ondrov.receipts.ui.viewmodels.ScanReceiptViewModel
import link.ondrov.receipts.util.FileUtils

@Composable
fun ScanReceiptRoute(
    onScanFinished: (String) -> Unit,
    onScanCancelled: () -> Unit,
    viewModel: ScanReceiptViewModel = viewModel(factory = ScanReceiptViewModel.Factory)
) {
    val context = LocalContext.current

    // 1. Scanner Setup
    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    // 2. Launcher for the Scanner Activity
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.firstOrNull()?.let { page ->
                // Copy file to internal storage and start analysis
                val permanentFile = FileUtils.copyUriToInternalStorage(context, page.imageUri)
                if (permanentFile != null) {
                    viewModel.analyzeImage(permanentFile)
                } else {
                    // Fail gracefully if file copy failed
                    onScanCancelled()
                }
            }
        } else {
            // User cancelled scanner
            onScanCancelled()
        }
    }

    // 3. Launch Scanner ONCE when entering this screen
    LaunchedEffect(Unit) {
        // Only launch if we aren't already analyzing or showing an error
        if (!viewModel.isAnalyzing && !viewModel.showRetryDialog) {
            scanner.getStartScanIntent(context as Activity)
                .addOnSuccessListener {
                    scannerLauncher.launch(IntentSenderRequest.Builder(it).build())
                }
                .addOnFailureListener {
                    onScanCancelled()
                }
        }
    }

    // 4. Navigation Event Listener (Success)
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { newReceiptId ->
            onScanFinished(newReceiptId)
        }
    }

    // 5. UI: Loading Overlay
    if (viewModel.isAnalyzing) {
        ExpressiveLoadingState(message = "Analyzing Receipt...")
    } else {
        // Empty box while waiting for scanner result or launching
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow))
    }

    // 6. UI: Retry Dialog
    if (viewModel.showRetryDialog) {
        ErrorRetryDialog(
            errorMessage = viewModel.lastErrorMessage,
            isApiKeyIssue = viewModel.isApiKeyMissingOrWrong,
            onDismiss = {
                viewModel.dismissError()
                onScanCancelled()
            },
            onRetry = { viewModel.retryAnalysis() }
        )
    }
}