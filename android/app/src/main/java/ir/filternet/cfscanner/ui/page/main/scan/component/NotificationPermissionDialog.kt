package ir.filternet.cfscanner.ui.page.main.scan.component

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import ir.filternet.cfscanner.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionRequest(dismiss: () -> Unit = {}) {
    Dialog(onDismissRequest = {

    }) {
//      (LocalView.current.parent as DialogWindowProvider).window.setDimAmount(0f)
        val notifPermissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
        var noCounter by remember { mutableStateOf(0) }

        val descriptionText = when (noCounter) {
            0 -> stringResource(R.string.notification_permission_desc)
            1 -> stringResource(R.string.notification_permission_desc1)
            else -> ""
        }

        Card(
            Modifier
                .aspectRatio(1f)
                .fillMaxWidth(0.6f),
            shape = RoundedCornerShape(5.dp),
            elevation = 5.dp,
            backgroundColor = MaterialTheme.colors.background
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = "Notification Icon",
                    Modifier
                        .size(50.dp)
                        .background(MaterialTheme.colors.primary.copy(0.05f), RoundedCornerShape(50))
                        .padding(8.dp),
                    tint = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.notification_permission),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.height(25.dp))
                Text(
                    text = descriptionText,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    Text(
                        text = stringResource(R.string.i_agree),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .width(100.dp)
                            .shadow(5.dp, RoundedCornerShape(50))
                            .background(MaterialTheme.colors.primary, RoundedCornerShape(50.dp))
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                notifPermissionState.launchPermissionRequest()
                                dismiss()
                            }
                            .padding(10.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.background
                    )


                    Text(
                        text = stringResource(R.string.no),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .width(80.dp)
                            .background(MaterialTheme.colors.error.copy(0.1f), RoundedCornerShape(50.dp))
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                noCounter++
                                if (noCounter == 2) {
                                    dismiss()
                                }
                            }
                            .padding(10.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.error
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
