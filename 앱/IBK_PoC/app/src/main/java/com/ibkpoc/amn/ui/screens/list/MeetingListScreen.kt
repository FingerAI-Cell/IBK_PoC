// ui/screens/list/MeetingListScreen.kt
package com.ibkpoc.amn.ui.screens.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import com.ibkpoc.amn.model.MeetingListResponse
import com.ibkpoc.amn.ui.navigation.Screen
import com.ibkpoc.amn.ui.screens.list.components.MeetingItem
import com.ibkpoc.amn.viewmodel.MeetingListViewModel

@Composable
fun MeetingListScreen(
    viewModel: MeetingListViewModel = hiltViewModel(),
    onNavigate: (Screen) -> Unit
) {
    val meetings by viewModel.meetings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = { onNavigate(Screen.Main()) }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "뒤로가기"
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(meetings) { meeting ->
                    MeetingItem(
                        meeting = meeting,
                        onClick = { 
                            onNavigate(Screen.Detail(
                                meetingId = meeting.id,
                                title = meeting.title,
                                start = meeting.start,
                                end = meeting.end,
                                participant = meeting.participant,
                                topic = meeting.topic
                            ))
                        }
                    )
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