// ui/theme/Type.kt
package com.ibkpoc.amn.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,  // 기본 SansSerif로 변경
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,  // 기본 SansSerif로 변경
        fontWeight = FontWeight.Bold,  // 굵은 글씨로 강조
        fontSize = 24.sp,  // 제목 크기 약간 확대
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,  // 기본 SansSerif로 변경
        fontWeight = FontWeight.SemiBold,  // 약간 굵게
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    )
)