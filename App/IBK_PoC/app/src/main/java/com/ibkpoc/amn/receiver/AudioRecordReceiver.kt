package com.ibkpoc.amn.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ibkpoc.amn.util.Logger
import java.io.File
import java.io.FileOutputStream

class AudioRecordReceiver : BroadcastReceiver() {

    companion object {
        var currentRecordFile: File? = null
        private var fileOutputStream: FileOutputStream? = null

        fun closeFileOutputStream() {
            try {
                fileOutputStream?.close()
                fileOutputStream = null
                Logger.e("파일 스트림 닫기 성공")
            } catch (e: Exception) {
                Logger.e("파일 스트림 닫기 실패: ${e.message}")
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.ibkpoc.amn.action.RECORDING_DATA") {
            intent.getByteArrayExtra("com.ibkpoc.amn.extra.AUDIO_DATA")?.let { data ->
                Logger.e("AudioRecordReceiver: 데이터 수신 (${data.size} bytes)")
                saveAudioData(data)
            }
        }
    }

    private fun saveAudioData(data: ByteArray) {
        try {
            if (fileOutputStream == null) {
                currentRecordFile?.let { file ->
                    fileOutputStream = FileOutputStream(file, true)
                } ?: throw IllegalStateException("녹음 파일이 설정되지 않았습니다")
            }
            fileOutputStream?.write(data)
            fileOutputStream?.flush()
        } catch (e: Exception) {
            Logger.e("오디오 데이터 저장 실패: ${e.message}")
        }
    }
}
