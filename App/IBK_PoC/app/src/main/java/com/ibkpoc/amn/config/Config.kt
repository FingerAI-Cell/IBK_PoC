package com.ibkpoc.amn.config

object Config {
    const val BASE_URL = "https://ibkpoc.fingerservice.co.kr/" // 서버 주소 "http://10.0.2.2:8080/" //
    const val TIMEOUT = 60L // 네트워크 타임아웃 설정 (초 단위)
    const val RECORD_INTERVAL = 5000L // 녹음 저장 간격 (밀리초 단위)
    const val DURATION_UPDATE_INTERVAL = 1000L // 녹음 시간 업데이트 간격 추가
}
