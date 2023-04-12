package ir.filternet.cfscanner.ui.page.sub.scan.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp

@Composable
fun AddNewConfigItem(modifier: Modifier = Modifier, rotation: Float, click: () -> Unit = {}) {
    Card(
        modifier.padding(horizontal = 6.dp),
        shape = RoundedCornerShape(50),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.primary
    ) {
        Box(modifier = Modifier.clickable { click() }.padding(vertical = 5.dp, horizontal = 30.dp), contentAlignment = Alignment.Center) {
            Image(
                Icons.Rounded.Add, "Add",
                Modifier.rotate(45-(45 * rotation)),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary),
            )
        }

    }
}