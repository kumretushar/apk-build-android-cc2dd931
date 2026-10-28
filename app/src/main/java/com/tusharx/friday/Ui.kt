@file:OptIn(ExperimentalMaterial3Api::class)

package com.tusharx.friday

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap

val Bg = Color(0xFF060A18)
val Panel = Color(0xFF0E1530)
val Cyan = Color(0xFF00E5FF)
val Purple = Color(0xFF9B5CFF)
val TextMain = Color(0xFFE6EDFF)
val Muted = Color(0xFF8A96BD)

@Composable
fun FridayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cyan, secondary = Purple, background = Bg, surface = Panel,
            onSurface = TextMain, onBackground = TextMain, onPrimary = Color.Black
        ),
        content = content
    )
}

@Composable
fun MainScreen(vm: FridayViewModel, onSettings: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.size - 1)
    }
    val chips = listOf("YouTube kholo", "Torch on karo", "Ek joke sunao", "Python code likho", "Kuch interesting batao")

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bg, Color(0xFF0B1030))))
            .statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("FRIDAY", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
                Text(
                    (if (vm.isBoss) "BOSS MODE" else "GUEST MODE") + "  •  " + when (vm.status) {
                        Status.IDLE -> "tap orb to talk"
                        Status.LISTENING -> "listening..."
                        Status.THINKING -> "thinking..."
                        Status.SPEAKING -> "speaking..."
                    },
                    color = Muted, fontSize = 12.sp
                )
            }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextMain) }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (vm.messages.isEmpty()) {
                item {
                    Text(
                        "Namaste! Main Friday hoon. Settings mein Gemini API key daalo, phir orb dabao aur bolo: \"YouTube par songs chalao\" ya \"Python mein calculator likho\".",
                        color = Muted, modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
            items(vm.messages) { Bubble(it) }
        }

        if (vm.partial.isNotBlank()) {
            Text(vm.partial, color = Cyan, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        }

        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            Orb(vm.status, vm.live) { vm.toggleLive() }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(chips) { c ->
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Panel)
                        .border(1.dp, Purple.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .clickable { vm.send(c) }.padding(horizontal = 14.dp, vertical = 8.dp)
                ) { Text(c, color = TextMain, fontSize = 13.sp) }
            }
        }

        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type karo ya orb dabao...", color = Muted) },
                shape = RoundedCornerShape(26.dp), maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { vm.send(input); input = "" })
            )
            IconButton(onClick = { vm.send(input); input = "" }) { Icon(Icons.Default.Send, contentDescription = "Send", tint = Cyan) }
        }
    }
}

@Composable
fun Orb(status: Status, live: Boolean, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "orb")
    val speed = when (status) { Status.SPEAKING -> 450; Status.LISTENING -> 800; Status.THINKING -> 600; else -> 1800 }
    val pulse by t.animateFloat(0.92f, 1.12f, infiniteRepeatable(tween(speed), RepeatMode.Reverse), label = "pulse")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(if (status == Status.THINKING) 1500 else 6000, easing = LinearEasing)), label = "rot")
    val core = when (status) { Status.LISTENING -> Cyan; Status.SPEAKING -> Purple; Status.THINKING -> Color(0xFFFFC857); else -> Color(0xFF3D6BFF) }

    Canvas(Modifier.size(150.dp).pointerInput(Unit) { detectTapGestures { onClick() } }) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2 * 0.5f * pulse
        drawCircle(Brush.radialGradient(listOf(core.copy(alpha = 0.55f), Purple.copy(alpha = 0.2f), Color.Transparent), center = c, radius = r * 2.2f), radius = r * 2.2f, center = c)
        drawCircle(Brush.radialGradient(listOf(Color.White, core, Purple), center = c, radius = r), radius = r, center = c)
        rotate(rot, c) {
            drawArc(
                color = if (live) Cyan else Purple, startAngle = 0f, sweepAngle = 100f, useCenter = false,
                topLeft = Offset(c.x - r * 1.35f, c.y - r * 1.35f), size = Size(r * 2.7f, r * 2.7f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun Bubble(m: Msg) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.fromUser) Alignment.End else Alignment.Start) {
        if (m.text.isNotBlank()) {
            val shape = RoundedCornerShape(18.dp)
            val bg = if (m.fromUser) Brush.linearGradient(listOf(Purple, Color(0xFF5B3CFF))) else Brush.linearGradient(listOf(Panel, Panel))
            Box(
                Modifier.widthIn(max = 310.dp).clip(shape).background(bg)
                    .then(if (m.fromUser) Modifier else Modifier.border(1.dp, Cyan.copy(alpha = 0.35f), shape))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) { Text(m.text, color = TextMain, fontSize = 15.sp) }
        }
        if (m.code.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF070B14)).border(1.dp, Purple.copy(alpha = 0.5f), RoundedCornerShape(14.dp))) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(m.codeLang.ifBlank { "code" }, color = Cyan, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("code", m.code))
                        Toast.makeText(ctx, "Code copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy") }
                }
                Text(
                    m.code, color = Color(0xFFB8F4FF), fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)
                )
            }
        }
        if (m.note.isNotBlank()) {
            Text(m.note, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp, start = 4.dp))
        }
    }
}
