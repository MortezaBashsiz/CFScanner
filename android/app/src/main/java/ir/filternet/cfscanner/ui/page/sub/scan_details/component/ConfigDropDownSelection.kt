package ir.filternet.cfscanner.ui.page.sub.scan_details.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.filternet.cfscanner.model.Config

@Composable
fun ConfigDropDown(expanded:Boolean = false, configs:List<Config> = emptyList(),
                   onSelect:(Config)->Unit = {},
                   onDismiss:()->Unit = {}
){
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onDismiss() },
    ) {
        val configs = configs.dropLast(1)
        val fraction = configs.size.coerceIn(1, 4)
        val size = (46.dp.value * fraction)
        Box(modifier = Modifier
            .width(150.dp)
            .height(Dp(size))) {
            LazyColumn {
                items(configs) { item ->
                    DropdownMenuItem(
                        onClick = {
                            onSelect(item)
                            onDismiss()
                        },
                    ) {
                        Text(text = item.name, color = MaterialTheme.colors.onBackground)
                    }
                }
            }
        }
    }

}