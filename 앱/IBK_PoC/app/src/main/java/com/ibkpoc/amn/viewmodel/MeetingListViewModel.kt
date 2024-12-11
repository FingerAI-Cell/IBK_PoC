package com.ibkpoc.amn.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ibkpoc.amn.model.*
import com.ibkpoc.amn.network.NetworkResult
import com.ibkpoc.amn.repository.MeetingRepository
import com.ibkpoc.amn.ui.navigation.Screen
import com.ibkpoc.amn.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val repository: MeetingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _meetings = MutableStateFlow<List<MeetingListResponse>>(emptyList())
    val meetings: StateFlow<List<MeetingListResponse>> = _meetings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        loadMeetings()
    }

    private fun loadMeetings() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getMeetingList().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val meetingList = result.data.map { response ->
                            MeetingListResponse(
                                id = response.id,
                                title = response.title,
                                start = response.start,
                                end = response.end,
                                participant = response.participant,
                                topic = response.topic
                            )
                        }
                        _meetings.value = meetingList
                        _isLoading.value = false
                    }
                    is NetworkResult.Error -> {
                        Logger.e("회의 목록 로딩 실패: ${result.message}", null, context)
                        _errorMessage.value = result.message
                        _isLoading.value = false
                    }
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }
}