package link.ondrov.receipts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import link.ondrov.receipts.ui.ReceiptAppTheme

// --- 1. Data Models ---
data class Category(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val transactionCount: Int,
    val totalSpent: Double,
    val budgetLimit: Double // Added to calculate progress
)

// --- 2. Main Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingCategoriesScreen() {
    // Mock Data State
    var categories by remember {
        mutableStateOf(
            listOf(
                Category("1", "Groceries", Icons.Rounded.ShoppingCart, 12, 450.50, 600.0),
                Category("2", "Transport", Icons.Rounded.DirectionsCar, 5, 120.00, 150.0),
                Category("3", "Entertainment", Icons.Rounded.Movie, 3, 85.00, 100.0),
                Category("4", "Dining Out", Icons.Rounded.Restaurant, 8, 230.00, 200.0), // Over budget example
                Category("5", "Health", Icons.Rounded.Favorite, 2, 45.00, 100.0),
            )
        )
    }

    // State for the Edit Sheet
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // Calculate Total for Header
    val totalSpent = categories.sumOf { it.totalSpent }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { }, // Empty title, we use the content below for a custom large header
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { /* Sort Action */ }) {
                        Icon(Icons.Rounded.Sort, contentDescription = "Sort")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Logic to add new category */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->

        Column(modifier = Modifier.padding(paddingValues)) {

            // --- Expressive Header ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Total Spending",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${"%.2f".format(totalSpent)}",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${categories.sumOf { it.transactionCount }} total receipts",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // --- Category List ---
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { category ->
                    CategoryCard(
                        category = category,
                        onClick = {
                            selectedCategory = category
                            showBottomSheet = true
                        }
                    )
                }
            }
        }
    }

    // --- Edit Bottom Sheet ---
    if (showBottomSheet && selectedCategory != null) {
        EditCategorySheet(
            category = selectedCategory!!,
            onDismiss = { showBottomSheet = false },
            onSave = { updatedCategory ->
                categories = categories.map { if (it.id == updatedCategory.id) updatedCategory else it }
                showBottomSheet = false
            }
        )
    }
}

// --- 3. Expressive Card Component ---
@Composable
fun CategoryCard(
    category: Category,
    onClick: () -> Unit
) {
    val progress = (category.totalSpent / category.budgetLimit).toFloat().coerceIn(0f, 1f)
    val isOverBudget = category.totalSpent > category.budgetLimit

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(24.dp), // Expressive high rounded corners
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "$${"%.2f".format(category.totalSpent)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${category.transactionCount} receipts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% of budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Visual Indicator
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

// --- 4. Edit Screen (Bottom Sheet) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategorySheet(
    category: Category,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(category.name) }
    var selectedIcon by remember { mutableStateOf(category.icon) }

    // Predefined Icons List
    val availableIcons = listOf(
        Icons.Rounded.ShoppingCart, Icons.Rounded.DirectionsCar, Icons.Rounded.Movie,
        Icons.Rounded.Restaurant, Icons.Rounded.Favorite, Icons.Rounded.Home,
        Icons.Rounded.Flight, Icons.Rounded.Pets, Icons.Rounded.Work,
        Icons.Rounded.School, Icons.Rounded.SportsEsports, Icons.Rounded.LocalCafe
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp) // Add bottom padding for gesture nav
        ) {
            Text(
                text = "Edit Category",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Select Icon",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Icon Picker Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(availableIcons) { icon ->
                    val isSelected = icon == selectedIcon
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedIcon = icon }
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Button(
                onClick = { onSave(category.copy(name = name, icon = selectedIcon)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSpendingScreen() {
    ReceiptAppTheme {
        SpendingCategoriesScreen()
    }
}