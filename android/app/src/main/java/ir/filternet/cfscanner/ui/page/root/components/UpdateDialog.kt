package ir.filternet.cfscanner.ui.page.root.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.rememberAsyncImagePainter
import ir.filternet.cfscanner.R
import ir.filternet.cfscanner.model.Update
import ir.filternet.cfscanner.model.UpdateState
import ir.filternet.cfscanner.ui.theme.Green
import ir.filternet.cfscanner.utils.clickableWithNoRipple
import ir.filternet.cfscanner.utils.round

@Composable
fun UpdateDialog(
    update: Update,
    download: () -> Unit = {},
    install: () -> Unit = {},
    cancel: () -> Unit = {},
    dismiss: () -> Unit = {},
) {
    Dialog(onDismissRequest = { dismiss() }) {
        ConstraintLayout(
            Modifier
                .wrapContentHeight()
                .clickableWithNoRipple { dismiss() }) {
            val cardRef = createRef()
            val iconRef = createRef()
            Card(
                Modifier
                    .fillMaxWidth(1f)
                    .height(IntrinsicSize.Min)
                    .constrainAs(cardRef) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .clickableWithNoRipple { },
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.fillMaxWidth().heightIn(min=300.dp,500.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = rememberAsyncImagePainter(R.drawable.update_bg),
                            contentDescription = "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(1.1f)
                        )
                        Text(text = stringResource(R.string.update_available), fontWeight = FontWeight.Bold, color = MaterialTheme.colors.background)

                        Text(
                            text = update.versionName,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.padding(top = 40.dp),
                            color = MaterialTheme.colors.background,
                            fontSize = 12.sp
                        )

                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            Modifier
                                .size(40.dp)
                                .padding(8.dp)
                                .clickable { dismiss() }
                                .background(MaterialTheme.colors.background.copy(0.2f), RoundedCornerShape(50))
                                .align(Alignment.TopEnd),
                            tint = MaterialTheme.colors.background
                        )
                    }

                    Text(
                        text = stringResource(R.string.what_s_changed),
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp),
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = update.changes.map { "● $it\n" }.joinToString("") { it },
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 17.dp, end = 8.dp),
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colors.onBackground.copy(0.9f)
                    )


                    Spacer(modifier = Modifier.height(8.dp))

                    UpdateButton(
                        update.state,
                        onClick = {
                            when (update.state) {
                                is UpdateState.Downloaded -> install()
                                else -> download()
                            }
                        }, onCancel = {
                            cancel()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                }

            }

            Image(
                painter = rememberAsyncImagePainter(R.drawable.ic_update_rocket),
                contentDescription = "",
                modifier = Modifier
                    .width(50.dp)
                    .constrainAs(iconRef) {
                        top.linkTo(cardRef.top)
                        bottom.linkTo(cardRef.top)
                        end.linkTo(cardRef.end)
                        start.linkTo(cardRef.start)

                    }
            )
        }

    }
}

@Composable
private fun UpdateButton(
    state: UpdateState,
    onClick: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    val height by animateDpAsState(targetValue = if (state !is UpdateState.Downloading) 45.dp else 4.dp)
    val elev by animateDpAsState(targetValue = if (state !is UpdateState.Downloading) 8.dp else 0.dp)
    val color by animateColorAsState(
        targetValue = when (state) {
            is UpdateState.Downloading -> MaterialTheme.colors.primaryVariant.copy(0.4f)
            is UpdateState.Downloaded -> Green
            else -> MaterialTheme.colors.primaryVariant
        }
    )
    val process = (state as? UpdateState.Downloading)?.progress ?: 0f
    val text = when (state) {
        is UpdateState.Idle -> stringResource(R.string.download_update)
        is UpdateState.Downloading -> ""
        is UpdateState.Downloaded -> stringResource(R.string.install_update)
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
    ) {

        val (buttonRef, cancelRef, percentRef) = createRefs()

        // button
        Card(
            Modifier
                .width(170.dp)
                .height(height)
                .constrainAs(buttonRef) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            shape = RoundedCornerShape(50),
            backgroundColor = color,
            elevation = elev
        ) {
            Box(Modifier.fillMaxSize()) {


                AnimatedVisibility(
                    visible = state !is UpdateState.Downloading,
                    exit = fadeOut(),
                    enter = fadeIn()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                onClick()
                            }, contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colors.background
                        )
                    }
                }


                // progress
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth(process)
                        .height(4.dp)
                        .background(MaterialTheme.colors.primary, RoundedCornerShape(50))
                )
            }
        }


        // percent text
        AnimatedVisibility(height == 4.dp, Modifier
            .constrainAs(percentRef) {
                start.linkTo(buttonRef.start)
                end.linkTo(buttonRef.end)
                top.linkTo(buttonRef.bottom)
            }
            .padding(top = 4.dp),
            enter = fadeIn(),
            exit = fadeOut(tween(50))
        ) {
            Text(
                text = "${(process * 100).round()}%",
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                color = MaterialTheme.colors.primaryVariant,
            )
        }


        // cancel button
        AnimatedVisibility(height == 4.dp, Modifier
            .constrainAs(cancelRef) {
                start.linkTo(buttonRef.end)
                top.linkTo(buttonRef.top)
                bottom.linkTo(buttonRef.bottom)
            }
            .padding(start = 4.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Cancel",
                Modifier
                    .size(30.dp)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colors.primaryVariant.copy(0.5f), RoundedCornerShape(50))
                    .clickable { onCancel() }
                    .padding(4.dp),
                tint = MaterialTheme.colors.background
            )
        }

    }


}
