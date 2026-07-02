package com.widgetchat.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.widgetchat.app.ui.theme.ChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(locale: String, colors: ChatColors, onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    // Codes MUST match the backend enum exactly, else the report is rejected (HTTP 400).
    val reasons = listOf(
        "not_relevant" to "Not relevant", "not_accurate" to "Not accurate",
        "too_repetitive" to "Too repetitive", "harmful_or_offensive" to "Harmful or offensive",
        "something_else" to "Something else",
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Strings.get(Strings.Key.BadResponse, locale), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(Strings.get(Strings.Key.ReportReason, locale), color = Color.Gray, fontSize = 13.sp)
            reasons.forEach { (code, label) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable { reason = code }.padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, Modifier.weight(1f))
                    if (reason == code) Icon(Icons.Filled.Check, null, tint = colors.primary)
                }
            }
            OutlinedTextField(value = note, onValueChange = { note = it },
                label = { Text(Strings.get(Strings.Key.ReportNote, locale)) },
                modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(Strings.get(Strings.Key.Cancel, locale)) }
                TextButton(onClick = { onSubmit(reason, note) }) { Text(Strings.get(Strings.Key.Submit, locale)) }
            }
        }
    }
}
