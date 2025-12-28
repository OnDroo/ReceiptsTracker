package link.ondrov.receipts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import link.ondrov.receipts.ui.ReceiptAppTheme
import link.ondrov.receipts.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enables edge-to-edge for the transparent status bar look
        enableEdgeToEdge()

        setContent {
            ReceiptAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    AppNavigation()
                }
            }
        }
    }
}