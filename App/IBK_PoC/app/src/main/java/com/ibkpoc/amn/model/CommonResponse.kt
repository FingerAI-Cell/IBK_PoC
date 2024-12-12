// model/CommonResponse.kt
package com.ibkpoc.amn.model

data class CommonResponse<T>(
    val status: String,
    val message: String,
    val data: T? = null
)