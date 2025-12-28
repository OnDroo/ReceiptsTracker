package link.ondrov.receipts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import link.ondrov.receipts.ui.theme.ReceiptsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsScreen() {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Receipts",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Open Drawer */ }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Open Profile */ }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { /* Capture Receipt */ },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture Receipt",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // 1. Search Bar
            SearchBar()
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Filter Buttons
            FilterRow()
            Spacer(modifier = Modifier.height(24.dp))

            // 3. Receipts List Container
            ReceiptsListCard()
        }
    }
}

@Composable
fun SearchBar() {
    var text by remember { mutableStateOf("") }
    // Surface Variant is standard for M3 input fields/search bars
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    BasicTextField(
        value = text,
        onValueChange = { text = it },
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        ),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(backgroundColor, RoundedCornerShape(28.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (text.isEmpty()) {
                        Text(
                            "Search by store, item...",
                            color = contentColor
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
fun FilterRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active State (e.g., Sorted) -> Use Secondary Container
        FilterButton(
            text = "Sort",
            icon = Icons.Default.Sort,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

        // Inactive State -> Use Surface Variant (subtle grey)
        FilterButton(
            text = "Date",
            icon = Icons.Default.KeyboardArrowDown,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FilterButton(
            text = "Category",
            icon = Icons.Default.KeyboardArrowDown,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FilterButton(
    text: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Button(
        onClick = { /* Filter Action */ },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (text == "Sort") {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            } else {
                Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ReceiptsListCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        // Surface color ensures it stands out slightly from the background
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        LazyColumn(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            val receipts = getMockReceipts()
            itemsIndexed(receipts) { index, receipt ->
                ReceiptItem(receipt)
                if (index < receipts.lastIndex) {
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptItem(receipt: Receipt) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box - Using Secondary Container for the pastel circle look
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = receipt.icon,
                contentDescription = null,
                // The content on top of secondary container
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = receipt.storeName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = receipt.date,
                style = MaterialTheme.typography.bodySmall,
                // OnSurfaceVariant is used for secondary text (grey)
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = receipt.amount,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// --- Data Models ---
data class Receipt(
    val storeName: String,
    val date: String,
    val amount: String,
    val icon: ImageVector
)

fun getMockReceipts(): List<Receipt> {
    return listOf(
        Receipt("Whole Foods Market", "October 26, 2023", "$45.82", Icons.Outlined.ShoppingCart),
        Receipt("The Corner Bistro", "October 25, 2023", "$88.15", Icons.Outlined.Restaurant),
        Receipt("American Airlines", "October 24, 2023", "$341.50", Icons.Outlined.Flight),
        Receipt("Shell Gas", "October 23, 2023", "$55.20", Icons.Outlined.LocalGasStation),
    )
}

@Preview(showBackground = true)
@Composable
fun ReceiptsScreenPreview() {
    // Wrapped in your specific theme to test dynamic colors
    ReceiptsTheme {
        ReceiptsScreen()
    }
}