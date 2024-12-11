// ui/screens/meeting/MeetingScreen.kt
package com.ibkpoc.amn.ui.screens.meeting

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ibkpoc.amn.ui.navigation.Screen
import com.ibkpoc.amn.ui.screens.meeting.components.RecordingIndicator
import androidx.hilt.navigation.compose.hiltViewModel
import com.ibkpoc.amn.viewmodel.MainViewModel
import androidx.compose.runtime.*
import com.ibkpoc.amn.R
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import android.view.View
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MeetingScreen(
    meetingId: Long,
    userId: Int,
    viewModel: MainViewModel,
    onNavigate: (Screen) -> Unit
) {
    var muteLayoutVisible by remember { mutableStateOf(false) }
    
    // 세션 초기화는 화면 진입 시 한 번만
    LaunchedEffect(Unit) {
        // 세션이 있으면 녹음 시작
        viewModel.currentMeetingSession.value?.let { session ->
            viewModel.startMeetingRecording(session.convId, session.userId)
        }
    }

    val currentSession by viewModel.currentMeetingSession.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (currentSession == null) {
        LaunchedEffect(Unit) {
            println("MeetingScreen: currentSession is null, navigating to Main")
            onNavigate(Screen.Main())
        }
    }else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 회의 번호 - 왼쪽 상단
            Text(
                text = "회의 번호: $meetingId",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.TopStart)
            )

            // 녹음 중 - 우측 상단
            RecordingIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )

            // 중앙 문구
            Text(
                text = "회의 내용은 실시간으로 녹음되어\n요약 후 회의록으로 작성됩니다",
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )

            // 음소거 버튼 추가 - 종료 버튼 위에 배치
            val isMuted by viewModel.isMuted.collectAsState()
            
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 음소거 버튼
                Button(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isMuted) "음소거 해제" else "음소거")
                }
                
                if (isMuted) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { context ->
                            LayoutInflater.from(context).inflate(R.layout.mute_indicator, null).apply {
                                val muteLayout = this.findViewById<RelativeLayout>(R.id.muteLayout)
                                muteLayout.visibility = View.VISIBLE
                            }
                        }
                    )
                }

                // 기존 종료 버튼
                Button(
                    onClick = { viewModel.endMeeting(onNavigate) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Text("회의 종료")
                    }
                }
            }
        }
    }
}