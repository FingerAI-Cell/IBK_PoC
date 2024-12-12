// ui/screens/meeting/components/RecordingIndicator.kt
package com.ibkpoc.amn.ui.screens.meeting.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@Composable
fun RecordingIndicator(
    modifier: Modifier = Modifier // 기본 Modifier 추가
) {
    Row(
        modifier = modifier, // 전달받은 Modifier 적용
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.scale(animateFloatAsState(
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                )
            ).value)
        )
        Text(
            text = "녹음중",
            color = MaterialTheme.colorScheme.error
        )
    }
}
