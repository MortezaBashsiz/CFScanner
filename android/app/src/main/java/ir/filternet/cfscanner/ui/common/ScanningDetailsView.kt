package ir.filternet.cfscanner.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ScanningDetailsView(icon: ImageVector, text: String, atStart: Boolean = true, color: Color = MaterialTheme.colors.onBackground) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (atStart)
            Icon(icon, contentDescription = "", tint = color)
        Text(
            text,
            Modifier
                .padding(vertical = 5.dp)
                .padding(start = 4.dp),
            style = LocalTextStyle.current.copy(color = color, fontWeight = FontWeight.Bold)
        )
        if (!atStart)
            Icon(icon, contentDescription = "", tint = color)
    }

}