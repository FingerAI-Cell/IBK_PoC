package com.ibkpoc.amn.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private const val TAG = "AMN_APP"
    private const val ERROR_LOG_FILE = "amn_error_log.txt"
    
    fun e(message: String, throwable: Throwable? = null, context: Context) {
        Log.e(TAG, message, throwable)
        
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val errorLog = buildString {
                append("[$timestamp] ERROR: $message")
                throwable?.let {
                    append("\nStacktrace: ${it.stackTraceToString()}")
                }
                append("\n\n")
            }
            
            val logFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                ERROR_LOG_FILE
            )
            logFile.appendText(errorLog)
            
        } catch (e: Exception) {
            Log.e(TAG, "로그 파일 작성 실패", e)
        }
    }
} 