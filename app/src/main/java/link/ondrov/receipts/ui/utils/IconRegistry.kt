package link.ondrov.receipts.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconRegistry {
    val availableIcons = mapOf(
        "shopping_cart" to Icons.Outlined.ShoppingCart,
        "restaurant" to Icons.Outlined.Restaurant,
        "coffee" to Icons.Outlined.LocalCafe,
        "bar" to Icons.Outlined.LocalBar,
        "fastfood" to Icons.Outlined.Fastfood,
        "bus" to Icons.Outlined.DirectionsBus,
        "car" to Icons.Outlined.DirectionsCar,
        "gas" to Icons.Outlined.LocalGasStation,
        "flight" to Icons.Outlined.Flight,
        "gym" to Icons.Outlined.FitnessCenter,
        "health" to Icons.Outlined.LocalHospital,
        "pharmacy" to Icons.Outlined.LocalPharmacy,
        "tech" to Icons.Outlined.Computer,
        "phone" to Icons.Outlined.PhoneAndroid,
        "home" to Icons.Outlined.Home,
        "utilities" to Icons.Outlined.Lightbulb,
        "pet" to Icons.Outlined.Pets,
        "gift" to Icons.Outlined.CardGiftcard,
        "education" to Icons.Outlined.School,
        "work" to Icons.Outlined.Work
    )

    fun getIcon(id: String): ImageVector {
        return availableIcons[id] ?: Icons.AutoMirrored.Outlined.HelpOutline
    }

    fun getId(icon: ImageVector): String {
        return availableIcons.entries.firstOrNull { it.value == icon }?.key ?: "shopping_cart"
    }
}