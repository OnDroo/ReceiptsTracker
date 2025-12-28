package link.ondrov.receipts.ui.utils

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * A helper to create ViewModels with arguments (Dependency Injection).
 * Usage:
 * val vm: MyViewModel = viewModelWithFactory { extras ->
 * MyViewModel(repo, extras.createSavedStateHandle())
 * }
 */
@Composable
inline fun <reified T : ViewModel> viewModelWithFactory(
    crossinline builder: (CreationExtras) -> T
): T {
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return builder(extras) as T
            }
        }
    )
}