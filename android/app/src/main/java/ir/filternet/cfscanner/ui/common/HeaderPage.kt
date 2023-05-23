package ir.filternet.cfscanner.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.utils.mirror

@Composable
fun HeaderPage(title: String,modifier:Modifier = Modifier, onBackPress: () -> Unit = {}) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.ArrowBackIos, contentDescription = null,
            Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClickLabel = stringResource(id = R.string.back)) {
                    onBackPress()
                }
                .background(MaterialTheme.colors.primary.copy(alpha = 0.05f))
                .align(Alignment.CenterStart)
                .padding(8.dp)
                .size(20.dp)
                .mirror(),
            tint = MaterialTheme.colors.primary
        )
        Text(text = title, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
    }
}