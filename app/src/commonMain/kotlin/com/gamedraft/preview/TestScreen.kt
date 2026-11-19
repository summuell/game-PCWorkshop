package com.gamedraft.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamedraft.preview.resources.Res
import com.gamedraft.preview.resources.screen_labels
import org.jetbrains.compose.resources.Font

@Composable
fun TestScreen() {
    var inProgress by rememberSaveable { mutableStateOf(false) }
    val title = if (inProgress) "进行中" else "测试"
    val action = if (inProgress) "返回主页" else "开始游戏"
    val labels = FontFamily(Font(Res.font.screen_labels, FontWeight.Bold))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF192844), Color(0xFF0B1223))))
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            BasicText(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = TextStyle(
                    color = Color(0xFFF4F7FF),
                    fontFamily = labels,
                    fontWeight = FontWeight.Bold,
                    fontSize = 52.sp,
                    textAlign = TextAlign.Center,
                ),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFA8F0D5))
                    .clickable(role = Role.Button) { inProgress = !inProgress }
                    .widthIn(min = 192.dp)
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = action,
                    style = TextStyle(
                        color = Color(0xFF112724),
                        fontFamily = labels,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}
