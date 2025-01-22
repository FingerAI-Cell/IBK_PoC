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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Build
import android.Manifest
import com.ibkpoc.amn.ui.screens.main.components.AudioPlayerDialog
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun MainScreen(
    recordingState: RecordServiceState,
    errorMessage: String?,
    isLoading: Boolean,
    showSuccessMessage: Boolean,
    elapsedTime: Long,
    onStartMeeting: (Int) -> Unit,
    onEndMeeting: () -> Unit,
    onMessageShown: () -> Unit,
    onPlayAudio: (String) -> Unit
) {
    var showRecordDialog by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var participantCount by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

//    // 에러 메시지 처리
//    LaunchedEffect(errorMessage) {
//        if (errorMessage != null) {
//            snackbarHostState.showSnackbar(
//                message = errorMessage,
//                duration = SnackbarDuration.Short
//            )
//            onMessageShown()
//        }
//    }

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
                        style = MaterialTheme.typography.headlineMedium.copy(color = Color.Black),  // 더 큰 글씨
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
                                    text = "녹음 시간: ${formatDuration(elapsedTime)}",
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
                                    ButtonBlack else ButtonGray,
                                contentColor = Color.White // 텍스트 색상
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
                            onClick = { showRecordDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = recordingState !is RecordServiceState.Recording,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (recordingState !is RecordServiceState.Recording) Color.Black else Color.Gray, // 텍스트 색상
                                disabledContentColor = Color.LightGray // 비활성화 상태 텍스트 색상
                            ),border = BorderStroke(
                                width = 1.dp,
                                color = if (recordingState !is RecordServiceState.Recording) Color.Gray else Color.LightGray // 테두리 색상 조정
                            ),
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

    // 기록 대화상자
    if (showRecordDialog) {
        AlertDialog(
            onDismissRequest = { showRecordDialog = false },
            title = {
                Text(
                    text = "녹음 파일 목록",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    MeetingRecordItem(
                        title = "회의 녹음 1",
                        date = "2024-12-20 14:53",
                        duration = "48:15",
                        onPlayClick = { 
                            onPlayAudio("audiorecord_20241220_145318.wav")
                        },
                        onSendClick = { /* 실제 전송 로직 */ }
                    )
                    MeetingRecordItem(
                        title = "회의 녹음 2",
                        date = "2024-12-19",
                        duration = "26:50",
                        onPlayClick = { 
                            onPlayAudio("ibk-poc-meeting_20241219_1.wav")
                        },
                        onSendClick = { /* 실제 전송 로직 */ }
                    )
                    MeetingRecordItem(
                        title = "회의 녹음 3",
                        date = "2024-12-19",
                        duration = "00:46",
                        onPlayClick = { 
                            onPlayAudio("IBK_Records/meeting_78.wav")
                        },
                        onSendClick = { /* 임시 */ }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecordDialog = false }) {
                    Text("닫기")
                }
            }
        )
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
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                viewModel.playPendingAudio()
            }
        }
    )

    val recordingState by viewModel.recordingState.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showSuccessMessage by viewModel.showSuccessMessage.collectAsState()
    val showPlayer by viewModel.showPlayer.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val currentFileName by viewModel.currentFileName.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // 재생 버튼 클릭 핸들러
    val onPlayClick = { fileName: String ->
        when {
            // Android 13 이상
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        viewModel.playAudioFile(fileName)
                    }
                    else -> {
                        viewModel.setPendingAudioFile(fileName)
                        launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                    }
                }
            }
            // Android 13 미만
            else -> {
                when {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        viewModel.playAudioFile(fileName)
                    }
                    else -> {
                        viewModel.setPendingAudioFile(fileName)
                        launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
    }

    MainScreen(
        recordingState = recordingState,
        errorMessage = errorMessage,
        isLoading = isLoading,
        showSuccessMessage = showSuccessMessage,
        elapsedTime = elapsedTime,
        onStartMeeting = viewModel::startMeeting,
        onEndMeeting = viewModel::endMeeting,
        onMessageShown = viewModel::onMessageShown,
        onPlayAudio = onPlayClick
    )

    // 플레이어 다이얼로그
    if (showPlayer && currentFileName != null) {
        AudioPlayerDialog(
            fileName = currentFileName!!,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            onDismiss = viewModel::hidePlayer,
            onPlayPause = viewModel::togglePlayPause,
            onSeekTo = viewModel::seekTo,
            onForward = viewModel::forward10Seconds,
            onRewind = viewModel::rewind10Seconds
        )
    }
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

@Composable
private fun RecordItem(
    title: String,
    dateTime: String,
    duration: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateTime,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = Color.LightGray
        )
    }
}

@Composable
private fun MeetingRecordItem(
    title: String,
    date: String,
    duration: String,
    onPlayClick: () -> Unit,
    onSendClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = date,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = duration,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 재생 버튼
                OutlinedButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "재생",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                }
                
                // 전송 버튼
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            onSendClick()
                            delay(1000)
                            Toast.makeText(context, "서버에 전송됐습니다.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text(
                        "전송",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = Color.LightGray
        )
    }
}