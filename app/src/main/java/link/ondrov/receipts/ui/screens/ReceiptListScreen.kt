package link.ondrov.receipts.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import link.ondrov.receipts.ui.components.TransactionItem
import link.ondrov.receipts.ui.viewmodels.ReceiptListViewModel

@Composable
fun ReceiptListRoute(
    onBack: () -> Unit,
    onReceiptClick: (String) -> Unit,
    viewModel: ReceiptListViewModel = viewModel(factory = ReceiptListViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    ReceiptListScreen(
        state = state,
        searchQuery = query,
        selectedCategory = selectedCategory,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onCategorySelect = viewModel::onCategoryFilterSelect,
        onBack = onBack,
        onReceiptClick = onReceiptClick
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReceiptListScreen(
    state: link.ondrov.receipts.ui.viewmodels.ReceiptListUiState,
    searchQuery: String,
    selectedCategory: String?,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onBack: () -> Unit,
    onReceiptClick: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var active by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onSearch = {
                            active = false
                            focusManager.clearFocus()
                        },
                        expanded = false,
                        onExpandedChange = { active = it },
                        placeholder = { Text("Search stores or items...") },
                        leadingIcon = {
                            // Back button only visible when active or logic dictates
                            // If we want a persistent back button for the screen:
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Search Icon")
                            }
                        }
                    )
                },
                expanded = false,
                onExpandedChange = { active = it },
                shape = RoundedCornerShape(16.dp),
                colors = SearchBarDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                windowInsets = WindowInsets(0.dp) // Reset insets since we handled status bar manually
            ) {}
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 1. Dynamic Filter Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // "All" Chip
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategorySelect(null) },
                        label = { Text("All") },
                        leadingIcon = if (selectedCategory == null) {
                            { Icon(Icons.Default.FilterList, null, Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(50)
                    )
                }

                // Real Categories from Database
                items(state.availableCategories) { category ->
                    FilterChip(
                        selected = selectedCategory == category.id,
                        onClick = { onCategorySelect(category.id) },
                        label = { Text(category.name) },
                        shape = RoundedCornerShape(50)
                    )
                }
            }

            // 2. The List
            if (state.groupedReceipts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty()) "No receipts yet." else "No matches found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    state.groupedReceipts.forEach { (dateHeader, items) ->
                        stickyHeader {
                            DateHeader(text = dateHeader)
                        }

                        items(items) { item ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                TransactionItem(item = item, onClick = { onReceiptClick(item.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}