package link.ondrov.receipts.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import link.ondrov.receipts.ui.viewmodels.SettingsViewModel
import link.ondrov.receipts.ui.screens.ModelSelectionCard

@Composable
fun ErrorRetryDialog(
    errorMessage: String,
    isApiKeyIssue: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val availableModels by settingsViewModel.availableModels.collectAsState()
    val selectedModel by settingsViewModel.selectedModel.collectAsState()
    val currentApiKey by settingsViewModel.apiKey.collectAsState()

    var localApiKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }

    LaunchedEffect(localApiKey) {
        if (localApiKey != currentApiKey) {
            delay(500)
            settingsViewModel.updateApiKey(localApiKey)
        }
    }

    LaunchedEffect(currentApiKey) {
        if (currentApiKey.isNotBlank()) {
            settingsViewModel.fetchModels()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Analysis Failed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1. Show the Error
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                if (isApiKeyIssue) {
                    link.ondrov.receipts.ui.screens.ApiKeyInput(
                        value = localApiKey,
                        onValueChange = { localApiKey = it }
                    )
                } else {
                    Text(
                        text = "Try a different model:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    ModelSelectionCard(
                        availableModels = availableModels,
                        selectedModel = selectedModel,
                        onModelSelected = { settingsViewModel.updateSelectedModel(it) },
                        isLoading = availableModels.isEmpty()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Discard Receipt")
            }
        }
    )
}