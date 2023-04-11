package ir.filternet.cfscanner.ui.page.sub.scan.component

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.CIDR
import ir.filternet.cfscanner.model.Connection
import ir.filternet.cfscanner.ui.theme.Orange
import ir.filternet.cfscanner.ui.theme.OrangeLight

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectionList(connections: Map<CIDR, List<Connection>>?) {
    val header = connections?.keys?.toList()
    var openCidr by remember { mutableStateOf<CIDR?>(null) }

    LazyColumn(
        Modifier.fillMaxWidth(1f),
        verticalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(start = 15.dp, end = 15.dp, bottom = 80.dp),
        content = {
            repeat(header?.size ?: 0) {
                val cidr = header?.get(it)!!
                stickyHeader {
                    ConnectionListHeader(cidr, connections[cidr]?.size ?: 0, cidr.uid == openCidr?.uid) {
                        openCidr = if (openCidr?.uid == cidr.uid) null else cidr
                    }
                }
                if (cidr.uid == openCidr?.uid) {
                    val conns = connections[cidr]
                    itemsIndexed(conns!!) { i, con ->
                        ConnectionListChild(con, i == conns.size - 1)
                    }
                }
            }
        }
    )
}

@Composable
private fun LazyItemScope.ConnectionListHeader(cidr: CIDR, counts: Int = 0, opened: Boolean = false, click: () -> Unit = {}) {
    Card(
        Modifier
            .fillMaxWidth(1f)
            .padding(bottom = if (!opened) 10.dp else 0.dp)
            .clickable { click() },
        elevation = 5.dp,
        shape = RoundedCornerShape(5.dp),
        backgroundColor = Orange
    ) {
        Row(Modifier.padding(10.dp)) {
            Text(text = "${cidr.address}/${cidr.subnetMask}", Modifier.weight(1f))
            Text(text = "$counts")
        }
    }

}


@Composable
private fun ConnectionListOptions() {
    Row(
        Modifier
            .fillMaxWidth(1f)
            .padding(horizontal = 8.dp)
            .background(OrangeLight)
            .padding(10.dp)
    ) {
        Text(text = "192.168.1.1", Modifier.weight(1f))
//        Icon(Icons.Filled.Sync, contentDescription = "")
        Spacer(modifier = Modifier.width(5.dp))
//        Icon(Icons.Filled.CopyAll, contentDescription = "")
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.ConnectionListChild(connection: Connection, isLast: Boolean = true) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth(1f)
            .padding(horizontal = 8.dp)
            .padding(bottom = if (isLast) 10.dp else 0.dp)
            .background(OrangeLight, RoundedCornerShape(bottomEnd = if (isLast) 5.dp else 0.dp, bottomStart = if (isLast) 5.dp else 0.dp))
            .padding(10.dp)
            .animateItemPlacement(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = connection.ip, Modifier.weight(1f))
//        Icon(Icons.Filled.Sync, contentDescription = "",
//            Modifier
//                .size(34.dp)
//                .clickable { clipboard.setText(AnnotatedString(connection.ip)) }
//                .padding(5.dp)
//                .clip(RoundedCornerShape(4.dp))
//        )
        Spacer(modifier = Modifier.width(5.dp))
//        Icon(Icons.Filled.CopyAll, contentDescription = "",
//            Modifier
//                .size(34.dp)
//                .clickable {
//                    Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
//                    clipboard.setText(AnnotatedString(connection.ip))
//                }
//                .padding(5.dp)
//                .clip(RoundedCornerShape(4.dp))
//        )
    }

}