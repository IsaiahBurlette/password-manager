package com.securevault.app.ui.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.securevault.app.generator.PasswordGenerator
import com.securevault.app.generator.PendingGeneratedPassword
import com.securevault.app.security.PasswordStrengthAnalyzer
import com.securevault.app.ui.components.StrengthMeter
import com.securevault.app.ui.components.copySensitiveToClipboard
import kotlin.math.roundToInt

@Composable
fun GeneratorScreen(onSaveToVault: () -> Unit) {
    val context = LocalContext.current

    var length by remember { mutableFloatStateOf(18f) }
    var useUpper by remember { mutableStateOf(true) }
    var useLower by remember { mutableStateOf(true) }
    var useNumbers by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var excludeAmbiguous by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    fun regenerate() {
        val options = PasswordGenerator.Options(
            length = length.roundToInt(),
            useUpper = useUpper,
            useLower = useLower,
            useNumbers = useNumbers,
            useSymbols = useSymbols,
            excludeAmbiguous = excludeAmbiguous
        )
        password = PasswordGenerator.generate(options)
    }

    LaunchedEffect(length, useUpper, useLower, useNumbers, useSymbols, excludeAmbiguous) {
        regenerate()
    }

    val strength = remember(password) { PasswordStrengthAnalyzer.analyze(password) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Password Generator",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Slide to trade convenience for security, then fine-tune below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = password,
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { regenerate() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Regenerate")
                    }
                    IconButton(onClick = {
                        if (password.isNotEmpty()) copySensitiveToClipboard(context, "Password", password)
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                StrengthMeter(result = strength)
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Security level", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${length.roundToInt()} characters",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = length,
                    onValueChange = { length = it },
                    valueRange = 6f..64f,
                    steps = 57,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Weak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Very strong", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                ToggleRow("Uppercase letters (A-Z)", useUpper) { useUpper = it || (!useLower && !useNumbers && !useSymbols) }
                ToggleRow("Lowercase letters (a-z)", useLower) { useLower = it || (!useUpper && !useNumbers && !useSymbols) }
                ToggleRow("Numbers (0-9)", useNumbers) { useNumbers = it || (!useUpper && !useLower && !useSymbols) }
                ToggleRow("Symbols (!@#$%...)", useSymbols) { useSymbols = it || (!useUpper && !useLower && !useNumbers) }
                ToggleRow("Avoid ambiguous characters (l, 1, O, 0)", excludeAmbiguous) { excludeAmbiguous = it }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            OutlinedButton(
                onClick = { regenerate() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text(" New password", modifier = Modifier.padding(start = 6.dp))
            }
            Button(
                onClick = {
                    PendingGeneratedPassword.value = password
                    onSaveToVault()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .padding(start = 12.dp)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Text(" Save to vault", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}
