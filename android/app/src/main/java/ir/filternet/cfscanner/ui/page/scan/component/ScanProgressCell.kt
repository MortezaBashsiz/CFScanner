package ir.filternet.cfscanner.ui.page.scan.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.ScanProgress

@Composable
fun ScanProgressCell(progress: ScanProgress) {
    val total = if(progress.totalConnectionCount>0) progress.totalConnectionCount else 1
    val checked = (progress.checkConnectionCount / total) / 100f
    val success = (progress.successConnectionCount / total) / 100f
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(IntrinsicSize.Min)
    ) {
        Percentage(checked, success)
        Spacer(modifier = Modifier.height(10.dp))
        PercentageDetails(progress)
    }
}

@Composable
private fun PercentageDetails(progress: ScanProgress) {
    val total = if(progress.totalConnectionCount>0) progress.totalConnectionCount else 1
    val checked = (progress.checkConnectionCount / total) / 100f
    val success = (progress.successConnectionCount / total) / 100f

    Row(Modifier.fillMaxWidth()) {
        val color = MaterialTheme.colors.primary
        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(50)))

            Spacer(modifier = Modifier.height(5.dp))

            Text(text = stringResource(id = R.string.checked), fontSize = 13.sp, color = color)

            Spacer(modifier = Modifier.height(5.dp))

            Text(text = progress.checkConnectionCount.toString(), fontSize = 13.sp, color = color)

            Spacer(modifier = Modifier.height(3.dp))

            Text(text = "$checked%", fontSize = 9.sp , color = color)

        }

        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier
                .size(10.dp)
                .background(MaterialTheme.colors.onSurface, RoundedCornerShape(50)))

            Spacer(modifier = Modifier.height(5.dp))

            Text(text = stringResource(R.string.total), fontSize = 13.sp)

            Spacer(modifier = Modifier.height(5.dp))

            Text(text = progress.totalConnectionCount.toString(), fontSize = 13.sp)
        }

        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val color = MaterialTheme.colors.primaryVariant
            Spacer(modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(50)))

            Spacer(modifier = Modifier.height(5.dp))

            Text(text = stringResource(R.string.success) , fontSize = 13.sp, color = color)

            Spacer(modifier = Modifier.height(5.dp))

            Text(text = progress.successConnectionCount.toString(), fontSize = 13.sp, color = color)

            Spacer(modifier = Modifier.height(3.dp))

            Text(text = "$success%", fontSize = 9.sp , color = color)
        }

    }
}

@Composable
private fun Percentage(checked: Float = 0f, success: Float = 0f) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(50)),
        contentAlignment = Alignment.CenterStart
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.onSurface, RoundedCornerShape(50))
        )

        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(checked)
                .background(MaterialTheme.colors.primaryVariant, RoundedCornerShape(50))
        )

        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(success)
                .background(MaterialTheme.colors.primary, RoundedCornerShape(50))
        )

    }
}