package com.sloosh.tv.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.sloosh.tv.data.update.AppUpdateInfo
import com.sloosh.tv.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit,
    onStartUpdate: (
        onProgress: (progress: Float, downloadedMb: Float, totalMb: Float) -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedMb by remember { mutableFloatStateOf(0f) }
    var totalMb by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val updateButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            updateButtonFocusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .clip(ContinuousRoundedRectangle(24.dp))
                .background(GlassSurfaceDark)
                .padding(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Icon Badge
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(ContinuousRoundedRectangle(18.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDownloading) Icons.Default.Download else Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Доступно обновление",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(ContinuousCapsule)
                            .background(Color.White)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = updateInfo.newVersion,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Text(
                        text = "текущая ${updateInfo.currentVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Changelog Card
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .clip(ContinuousRoundedRectangle(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(14.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = updateInfo.changelog,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        lineHeight = 18.sp
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isDownloading) {
                    // Download Progress View
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(ContinuousCapsule),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format("%.1f MB / %.1f MB", downloadedMb, totalMb),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedDark
                            )
                            Text(
                                text = String.format("%d%%", (downloadProgress * 100).toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SlooshButton(
                            text = "Обновить",
                            isWhite = true,
                            onClick = {
                                isDownloading = true
                                errorMessage = null
                                onStartUpdate(
                                    { prog, dl, tot ->
                                        downloadProgress = prog
                                        downloadedMb = dl
                                        totalMb = tot
                                    },
                                    { err ->
                                        isDownloading = false
                                        errorMessage = err
                                    }
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(updateButtonFocusRequester)
                        )

                        SlooshButton(
                            text = "Позже",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
