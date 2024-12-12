// model/UserValidationResponse.kt (추가)
package com.ibkpoc.amn.model

import com.google.gson.annotations.SerializedName

data class UserValidationResponse(
    @SerializedName("userId") val userId: Int,
    @SerializedName("active") val active: Boolean,
    @SerializedName("valid") val isValid: Boolean,
    @SerializedName("meetingActive") val isMeetingActive: Boolean
)