// ui/screens/main/MainScreen.kt
package com.ibkpoc.amn.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.rounded.List
import com.ibkpoc.amn.model.RecordServiceState
import com.ibkpoc.amn.ui.screens.main.components.RecordingIndicator
import com.ibkpoc.amn.viewmodel.MainViewModel
import androidx.compose.foundation.shape.CircleShape
import com.ibkpoc.amn.ui.theme.StatusGreenBackground
import com.ibkpoc.amn.ui.theme.StatusGreenText
import com.ibkpoc.amn.ui.theme.ButtonBlack
import com.ibkpoc.amn.ui.theme.ButtonGray
import com.ibkpoc.amn.ui.theme.ButtonRed
import com.ibkpoc.amn.ui.theme.ButtonLightRed
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun MainScreen(
    recordingState: RecordServiceState,
    errorMessage: String?,
    isLoading: Boolean,
    showSuccessMessage: Boolean,
    elapsedTime: Long,
    onStartMeeting: (Int) -> Unit,
    onEndMeeting: () -> Unit,
    onMessageShown: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var participantCount by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // 에러 메시지 처리
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
            onMessageShown()
        }
    }

    // 녹음 상태에 따른 UI 표시
    when (recordingState) {
        is RecordServiceState.Recording -> {
            // 녹음 중일 때의 UI
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = StatusGreenText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "녹음 중입니다.",
                        color = StatusGreenText
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "회의 ID: ${recordingState.meetingId}",
                    color = StatusGreenText
                )
                Text(
                    text = "녹음 시간: ${formatDuration(elapsedTime)}",
                    color = StatusGreenText
                )
            }
        }
        is RecordServiceState.Completed -> {
            // 녹음 완료 시 UI
            if (showSuccessMessage) {
                LaunchedEffect(showSuccessMessage) {
                    Toast.makeText(context, "녹음이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    onMessageShown()
                }
            }
        }
        is RecordServiceState.Error -> {
            // 에러 발생 시 UI
            if (errorMessage != null) {
                LaunchedEffect(errorMessage) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    onMessageShown()
                }
            }
        }
        RecordServiceState.Idle -> {
            // 대기 상태 UI
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 성공 메시지를 회의 관리 텍스트 앞에 배치
                    if (showSuccessMessage) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = StatusGreenBackground
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = StatusGreenText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "회의가 저장되었습니다",
                                    color = StatusGreenText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))  // 상단 여백 추가
                    Text(
                        text = "회의 관리",
                        style = MaterialTheme.typography.headlineMedium,  // 더 큰 글씨
                        fontWeight = FontWeight.Bold,  // 굵은 글씨
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),  // 상하 여백 추가
                        textAlign = TextAlign.Center  // 중앙 정렬
                    )

                    // 버음 중 상태 표시
                    if (recordingState is RecordServiceState.Recording) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = StatusGreenBackground
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = StatusGreenText
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "녹음 중입니다.",
                                        color = StatusGreenText
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "회의 ID: ${recordingState.meetingId}",
                                    color = StatusGreenText
                                )
                                Text(
                                    text = "녹음 시간: ${formatDuration(recordingState.duration)}",
                                    color = StatusGreenText
                                )
                            }
                        }
                    }

                    // 버튼들을 중앙에 배치
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 회의 시작 버튼
                        MeetingButton(
                            text = "회의 시작",
                            icon = Icons.Rounded.PlayArrow,
                            onClick = { showDialog = true },
                            enabled = recordingState !is RecordServiceState.Recording && !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recordingState !is RecordServiceState.Recording) 
                                    ButtonBlack else ButtonGray
                            )
                        )

                        // 참가자 수 입력 다이얼로그
                        if (showDialog) {
                            AlertDialog(
                                onDismissRequest = { 
                                    showDialog = false 
                                    participantCount = ""
                                    showError = false
                                },
                                title = { Text("회의 참가자 수 입력") },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = participantCount,
                                            onValueChange = { 
                                                participantCount = it
                                                showError = false
                                            },
                                            label = { Text("참가자 수") },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number
                                            ),
                                            isError = showError,
                                            singleLine = true
                                        )
                                        if (showError) {
                                            Text(
                                                text = "1명 이상의 참가자 수를 입력해주세요",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val count = participantCount.toIntOrNull() ?: 0
                                        if (count > 0) {
                                            onStartMeeting(count)
                                            showDialog = false
                                            participantCount = ""
                                            showError = false
                                        } else {
                                            showError = true
                                        }
                                    }) {
                                        Text("확인")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { 
                                        showDialog = false
                                        participantCount = ""
                                        showError = false
                                    }) {
                                        Text("취소")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 회의 종료 버튼
                        MeetingButton(
                            text = "회의 종료",
                            icon = Icons.Rounded.Stop,
                            onClick = onEndMeeting,
                            enabled = recordingState is RecordServiceState.Recording && !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recordingState is RecordServiceState.Recording) 
                                    ButtonRed else ButtonLightRed,
                                disabledContainerColor = ButtonLightRed
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 기록 버튼
                        OutlinedButton(
                            onClick = { /* TODO */ },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color.Gray),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.List,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("기록")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetingButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    colors: ButtonColors
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val recordingState by viewModel.recordingState.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showSuccessMessage by viewModel.showSuccessMessage.collectAsState()

    MainScreen(
        recordingState = recordingState,
        errorMessage = errorMessage,
        isLoading = isLoading,
        showSuccessMessage = showSuccessMessage,
        elapsedTime = elapsedTime,
        onStartMeeting = viewModel::startMeeting,
        onEndMeeting = viewModel::endMeeting,
        onMessageShown = viewModel::onMessageShown
    )
}

@Composable
private fun RecordingIndicator(
    elapsedTime: String,
    onStopRecording: () -> Unit
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "녹음 중... $elapsedTime",
            style = MaterialTheme.typography.bodyLarge
        )
        IconButton(onClick = onStopRecording) {
            Icon(
                imageVector = Icons.Rounded.Stop,
                contentDescription = "녹음 중지"
            )
        }
    }
}