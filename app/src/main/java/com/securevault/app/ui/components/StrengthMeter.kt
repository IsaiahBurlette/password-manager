package com.securevault.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.securevault.app.security.CrackTimeEstimate
import com.securevault.app.security.PasswordStrengthResult
import com.securevault.app.security.StrengthLabel
import com.securevault.app.ui.theme.StrengthFair
import com.securevault.app.ui.theme.StrengthStrong
import com.securevault.app.ui.theme.StrengthVeryStrong
import com.securevault.app.ui.theme.StrengthVeryWeak
import com.securevault.app.ui.theme.StrengthWeak

fun strengthColor(label: StrengthLabel): Color = when (label) {
    StrengthLabel.VERY_WEAK -> StrengthVeryWeak
    StrengthLabel.WEAK -> StrengthWeak
    StrengthLabel.FAIR -> StrengthFair
    StrengthLabel.STRONG -> StrengthStrong
    StrengthLabel.VERY_STRONG -> StrengthVeryStrong
}

@Composable
fun StrengthMeter(
    result: PasswordStrengthResult,
    modifier: Modifier = Modifier,
    showCrackTimes: Boolean = true
) {
    val color by animateColorAsState(strengthColor(result.label), tween(300), label = "strengthColor")
    val progress by animateFloatAsState(
        (result.score / 100f).coerceIn(0f, 1f),
        tween(400),
        label = "strengthProgress"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = result.label.label,
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
            Text(
                text = "${result.entropyBits.toInt()} bits of entropy",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            ) {}
        }

        if (result.warnings.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                result.warnings.forEach { warning ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = StrengthWeak,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }

        if (showCrackTimes) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Estimated time to crack",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                result.crackTimes.forEach { CrackTimeRow(it) }
            }
        }
    }
}

@Composable
private fun CrackTimeRow(estimate: CrackTimeEstimate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(end = 8.dp)) {
            Text(
                text = estimate.scenario,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = estimate.displayTime,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StrengthDot(label: StrengthLabel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(strengthColor(label))
    ) {}
}
