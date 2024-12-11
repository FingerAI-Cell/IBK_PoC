// ui/screens/detail/MeetingDetailScreen.kt
package com.ibkpoc.amn.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ibkpoc.amn.ui.navigation.Screen
import com.ibkpoc.amn.viewmodel.MeetingDetailViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import com.ibkpoc.amn.model.MeetingListResponse
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun MeetingDetailScreen(
    meeting: MeetingListResponse,
    viewModel: MeetingDetailViewModel = hiltViewModel(),
    onNavigate: (Screen) -> Unit
) {
    val meetingDetail by viewModel.meetingDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(meeting.id) {
        viewModel.loadMeetingDetail(meeting.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        IconButton(
            onClick = { onNavigate(Screen.List()) }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "목록으로"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 기본 정보 섹션
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = meeting.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${meeting.start} ~ ${meeting.end}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "참가자 수: ${meeting.participant}명",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "주제: ${meeting.topic}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                meetingDetail?.let { detail ->
                    // 참가자 목록 섹션
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "참가자 목록",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = detail.participant?.toString() ?: "참가자 정보 없음",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // 회의 요약 섹션
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "회의 요약",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = detail.summary?.toString() ?: "요약 정보 없음",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}