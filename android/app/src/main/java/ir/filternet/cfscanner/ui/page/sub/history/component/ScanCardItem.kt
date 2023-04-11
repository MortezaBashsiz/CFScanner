package ir.filternet.cfscanner.ui.page.sub.history.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.filternet.cfscanner.model.Scan
import ir.filternet.cfscanner.model.ScanProgress
import ir.filternet.cfscanner.utils.parseToCommonName
import java.util.*


@Composable
fun ScanCardItem(scan: Scan, click: (scan: Scan) -> Unit = {}) {
    val founded = scan.progress.successConnectionCount
    Card(
        Modifier
            .fillMaxWidth(0.9f)
            .height(IntrinsicSize.Min),
        elevation = 4.dp,
        shape = RoundedCornerShape(5.dp),
        backgroundColor = MaterialTheme.colors.onSurface,
    ) {
        val context = LocalContext.current
        Row(
            Modifier
                .fillMaxSize()
                .clickable { click(scan) },
            verticalAlignment = Alignment.CenterVertically
        ) {

            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(MaterialTheme.colors.primary)
            )

            Row(
                Modifier
                    .weight(1f)
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(text = "${scan.config.name} | ${scan.isp.parseToCommonName(context)}")
                    Spacer(modifier = Modifier.height(5.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.History, contentDescription = "", Modifier.size(18.dp))
                        Text(text = dateFormat(scan.creationDate), fontSize = 11.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(4.dp))

                        Spacer(modifier = Modifier.weight(1f))

                        Icon(Icons.Outlined.CheckCircle, contentDescription = "", Modifier.size(18.dp))
                        Text(text = "$founded found", fontSize = 11.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(4.dp))

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                ScanProgressView(ScanProgress(100, 50, 20))
            }

            Spacer(modifier = Modifier.width(5.dp))

            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "")
        }
    }
}

@Composable
fun ScanProgressView(progress: ScanProgress) {
    val color = MaterialTheme.colors.primary
    val percentage = progress.run { checkConnectionCount / totalConnectionCount }
    val percent = percentage * 360f
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.drawContext.size.width
            val h = this.drawContext.size.height
            drawArc(color, -90f, percent, false, style = Stroke(7f, cap = StrokeCap.Round))
            drawArc(color.copy(0.1f), 0f, 360F, false, style = Stroke(7f, cap = StrokeCap.Square))
        }

        Text(text = "${percentage.toInt()}%", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = color)
    }
}

fun dateFormat(time: Date): String {
    val now = Calendar.getInstance().timeInMillis
    val result = android.text.format.DateUtils.getRelativeTimeSpanString(time.time, now, android.text.format.DateUtils.MINUTE_IN_MILLIS)
    return result.toString()
}