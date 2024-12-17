// ui/screens/main/MainScreen.kt
package com.ibkpoc.amn.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ibkpoc.amn.model.RecordingState
import com.ibkpoc.amn.ui.screens.main.components.RecordingIndicator
import com.ibkpoc.amn.viewmodel.MainViewModel

@Composable
fun MainScreen(
    recordingState: RecordingState,
    errorMessage: String?,
    isLoading: Boolean,
    onStartMeeting: () -> Unit,
    onEndMeeting: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (recordingState) {
            is RecordingState.Recording -> {
                RecordingIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "회의 ID: ${recordingState.meetingId}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "녹음 시간: ${formatDuration(recordingState.duration)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            RecordingState.Idle -> {
                Text(
                    text = "회의를 시작하려면\n아래 버튼을 눌러주세요",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            is RecordingState.Error -> {
                Text(
                    text = recordingState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (recordingState is RecordingState.Recording) {
            Button(
                onClick = onEndMeeting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("회의 종료")
            }
        } else {
            Button(
                onClick = onStartMeeting,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("회의 시작")
                }
            }
        }

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val recordingState = viewModel.recordingState.collectAsState().value
    val errorMessage = viewModel.errorMessage.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value

    MainScreen(
        recordingState = recordingState,
        errorMessage = errorMessage,
        isLoading = isLoading,
        onStartMeeting = { viewModel.startMeeting() },
        onEndMeeting = { viewModel.endMeeting() }
    )
}