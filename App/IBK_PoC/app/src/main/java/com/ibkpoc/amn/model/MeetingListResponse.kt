package com.ibkpoc.amn.model

import java.text.SimpleDateFormat
import java.util.Locale

data class MeetingListResponse(
    val id: Long,
    val title: String = "제목 없는 회의",
    val startTime: String?,
    val endTime: String?
) {
    fun getDuration(): String {
        if (startTime == null || endTime == null) {
            return "재생 시간 정보 없음"
        }
        
        return try {
            // 여러 가지 시간 포맷 시도
            val formats = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd-HH-mm-ss",
                "HH:mm:ss",
                "mm:ss"
            )
            
            var start: Long? = null
            var end: Long? = null
            
            for (format in formats) {
                try {
                    val formatter = SimpleDateFormat(format, Locale.getDefault())
                    start = formatter.parse(startTime)?.time
                    end = formatter.parse(endTime)?.time
                    if (start != null && end != null) break
                } catch (e: Exception) {
                    continue
                }
            }
            
            if (start != null && end != null) {
                val durationMillis = end - start
                val minutes = durationMillis / (1000 * 60)
                val seconds = (durationMillis / 1000) % 60
                String.format("%02d:%02d", minutes, seconds)
            } else {
                "재생 시간 정보 없음"
            }
        } catch (e: Exception) {
            "재생 시간 정보 없음"
        }
    }
} 