package ir.filternet.cfscanner.ui.page.sub.scan.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.Config
import ir.filternet.cfscanner.scanner.v2ray.ServerConfig
import ir.filternet.cfscanner.scanner.v2ray.V2rayConfigUtil

@Composable
fun ConfigEditView(config: Config, delete: (Config) -> Unit = {}) {

    val context = LocalContext.current
    val v2rayConfigUtil = remember { V2rayConfigUtil(context) }
    val Vconfig = v2rayConfigUtil.createServerConfig(config.config)

    Column(Modifier.fillMaxWidth(0.9f), horizontalAlignment = Alignment.CenterHorizontally) {

        Text(text = stringResource(R.string.config_details))
        Spacer(modifier = Modifier.height(20.dp))


        Vconfig?.let {
            ConfigDetails(it,config.config)

            Spacer(modifier = Modifier.height(8.dp))

            ConfigDelete {
                delete(config)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ConfigDetails(config: ServerConfig,text:String) {
    Card(
        Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.primary,
        shape = MaterialTheme.shapes.medium
    )
    {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(config.remarks)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text)
//            Text("${config.fullConfig?.outbounds?.firstOrNull()?.protocol} - ${config.fullConfig?.outbounds?.firstOrNull()?.streamSettings?.network}")
//            Spacer(modifier = Modifier.height(10.dp))
//            Text("${config.fullConfig?.outbounds?.firstOrNull()?.settings?.vnext?.firstOrNull()?.users?.firstOrNull()?.id}")
//            Spacer(modifier = Modifier.height(10.dp))
//            Text("${config.fullConfig?.outbounds?.firstOrNull()?.settings?.vnext?.firstOrNull()?.run { "$address : $port" }}")
//            Spacer(modifier = Modifier.height(10.dp))
//            Text("${config.fullConfig?.outbounds?.firstOrNull()?.streamSettings?.tlsSettings?.serverName}")
        }
    }
}

@Composable
private fun ConfigDelete(clicked: () -> Unit = {}) {
    Card(
        Modifier.fillMaxWidth().height(45.dp).clickable { clicked() },
        elevation = 4.dp,
        backgroundColor = Color.Red,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.delete_config), style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold))
        }
    }
}
