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
class MeetingDetailViewModel @Inject constructor(
    private val repository: MeetingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _meetingDetail = MutableStateFlow<MeetingDetailResponse?>(null)
    val meetingDetail = _meetingDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun loadMeetingDetail(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getMeetingDetail(id).collect { result ->
                _isLoading.value = false
                when (result) {
                    is NetworkResult.Success -> _meetingDetail.value = result.data
                    is NetworkResult.Error -> {
                        Logger.e("회의 상세 정보 로딩 실패 (ID: $id): ${result.message}", null, context)
                        _errorMessage.value = result.message
                    }
                    is NetworkResult.Loading -> { /* 이미 위에서 처리 */ }
                }
            }
        }
    }
} 