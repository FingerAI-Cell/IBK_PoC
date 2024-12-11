// ui/screens/main/MainScreen.kt
package com.ibkpoc.amn.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ibkpoc.amn.ui.navigation.Screen
import com.ibkpoc.amn.ui.screens.main.components.WarningCard
import com.ibkpoc.amn.viewmodel.MainViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigate: (Screen) -> Unit
) {
    var inputNumber by remember { mutableStateOf("") }
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val endMeetingSuccess by viewModel.endMeetingSuccess.collectAsState()

    // Snackbar 상태 관리
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    // 회의 종료 상태에 따라 Snackbar 표시
    LaunchedEffect(endMeetingSuccess) {
        if (endMeetingSuccess) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("회의가 종료되었습니다.")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 경고 메시지 카드
            WarningCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputNumber,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                inputNumber = newValue
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("숫자 입력 (1-10)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !isLoading
                    )

                    Button(
                        onClick = {
                            val userId = inputNumber.toIntOrNull()
                            if (userId != null) {
                                viewModel.validateAndEnterMeeting(userId) { screen ->
                                    onNavigate(screen)
                                }
                            } else {
                                viewModel.setErrorMessage("1에서 10 사이의 숫자를 입력하세요.")
                            }
                        },
                        modifier = Modifier.height(56.dp),
                        enabled = !isLoading && inputNumber.isNotEmpty()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Enter")
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

            Button(
                onClick = { onNavigate(Screen.List()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text("회의 목록")
            }

        }
    }
}