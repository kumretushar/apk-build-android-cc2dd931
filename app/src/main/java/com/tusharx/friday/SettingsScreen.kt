@file:OptIn(ExperimentalMaterial3Api::class)

package com.tusharx.friday

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(vm: FridayViewModel, onBack: () -> Unit) {
    val p = vm.prefs
    val ctx = LocalContext.current

    var apiKey by remember { mutableStateOf(p.apiKey) }
    var showKey by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf(p.model) }
    var boss by remember { mutableStateOf(p.bossName) }
    var owner by remember { mutableStateOf(p.ownerName) }
    var pass by remember { mutableStateOf(p.passphrase) }
    var guest by remember { mutableStateOf(p.guestControl) }
    var personality by remember { mutableStateOf(p.personality) }
    var language by remember { mutableStateOf(p.language) }
    var extra by remember { mutableStateOf(p.extra) }
    var rate by remember { mutableStateOf(p.rate) }
    var pitch by remember { mutableStateOf(p.pitch) }
    var silence by remember { mutableStateOf(p.silenceSec.toFloat()) }
    var proactive by remember { mutableStateOf(p.proactive) }
    var autoSpeak by remember { mutableStateOf(p.autoSpeak) }

    Column(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextMain) }
            Text("Settings", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Section("Gemini API") {
                OutlinedTextField(
                    value = apiKey, onValueChange = { apiKey = it; p.apiKey = it },
                    label = { Text("API key") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = showKey, onCheckedChange = { showKey = it }); Spacer(Modifier.width(8.dp)); Text("Show key", color = Muted)
                }
                OutlinedTextField(
                    value = model, onValueChange = { model = it; p.model = it },
                    label = { Text("Model (e.g. gemini-2.5-flash)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { vm.testApi() }) { Text("Test connection") }
                if (vm.apiTest.isNotBlank()) Text(vm.apiTest, color = Muted, fontSize = 13.sp)
            }

            Section("Owner & identity") {
                OutlinedTextField(
                    value = boss, onValueChange = { boss = it; p.bossName = it },
                    label = { Text("What Friday calls the owner") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = owner, onValueChange = { owner = it; p.ownerName = it },
                    label = { Text("Creator name (shown when asked who made it)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pass, onValueChange = { pass = it; p.passphrase = it },
                    label = { Text("Secret unlock phrase (blank = everyone is owner)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Owner banne ke liye bolo ye phrase. 'Friday lock' bolne par guest mode. Android voice ko pehchaan nahi sakta, isliye phrase use hota hai.",
                    color = Muted, fontSize = 12.sp
                )
                SwitchRow("Guests can control the phone", guest) { guest = it; p.guestControl = it }
            }

            Section("Personality") {
                Choice("Style", listOf("Friendly", "Funny", "Professional", "Desi Swag"), personality) { personality = it; p.personality = it }
                Choice("Language", listOf("Hinglish", "Hindi", "English"), language) { language = it; p.language = it; vm.applyVoiceSettings() }
                OutlinedTextField(
                    value = extra, onValueChange = { extra = it; p.extra = it },
                    label = { Text("Extra instructions for Friday") }, modifier = Modifier.fillMaxWidth(), minLines = 2
                )
            }

            Section("Voice") {
                SwitchRow("Speak replies aloud", autoSpeak) { autoSpeak = it; p.autoSpeak = it }
                SliderRow("Speed", rate, 0.5f..1.6f, "%.2f".format(rate)) { rate = it; p.rate = it }
                SliderRow("Pitch (low = deep)", pitch, 0.5f..1.5f, "%.2f".format(pitch)) { pitch = it; p.pitch = it }
                Button(onClick = { vm.testVoice() }) { Text("Test voice") }
            }

            Section("Proactive mode") {
                SwitchRow("Speak up when I'm silent", proactive) { proactive = it; p.proactive = it }
                SliderRow("Silence before speaking", silence, 15f..300f, "${silence.toInt()} sec") { silence = it; p.silenceSec = it.toInt() }
            }

            Section("Phone control") {
                Text(
                    "Home / Back / Recents / Screenshot ke liye Accessibility service ON karo (Friday ko list mein dhundo).",
                    color = Muted, fontSize = 12.sp
                )
                Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                    Text("Open Accessibility settings")
                }
                OutlinedButton(onClick = { vm.clearChat() }) { Text("Clear chat history") }
            }
            Spacer(Modifier.height(24.dp))
            Text("Friday • made by ${owner.ifBlank { "GM Tusharx" }}", color = Muted, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel)
            .border(1.dp, Purple.copy(alpha = 0.35f), RoundedCornerShape(18.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        content()
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMain, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, shown: String, onChange: (Float) -> Unit) {
    Column {
        Row { Text(label, color = TextMain, modifier = Modifier.weight(1f)); Text(shown, color = Muted) }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun Choice(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = TextMain)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { o ->
                val on = o == selected
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (on) Purple else Bg)
                        .border(1.dp, if (on) Cyan else Muted.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onSelect(o) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text(o, color = TextMain, fontSize = 12.sp) }
            }
        }
    }
}
