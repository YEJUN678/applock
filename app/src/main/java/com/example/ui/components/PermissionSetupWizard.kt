package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** 권한 1건의 현재 상태와 안내. */
data class PermissionStep(
    val id: String,
    val title: String,
    val requirement: String,
    val why: String,
    val granted: Boolean,
    val required: Boolean,
    val actionLabel: String
)

/**
 * 시작할 때 표시되는 권한 설정 튜토리얼.
 * 상태는 부모가 넘겨주고(설정 화면을 다녀온 뒤 갱신), 버튼만 눌러주면 된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupWizard(
    steps: List<PermissionStep>,
    onLaunchPermission: (PermissionStep) -> Unit,
    onFinish: () -> Unit
) {
    val grantedCount = steps.count { it.granted }
    val requiredMissing = steps.filter { it.required && !it.granted }
    val progress = if (steps.isEmpty()) 1f else grantedCount.toFloat() / steps.size
    val bodyMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.66f).dp

    ModalBottomSheet(
        onDismissRequest = onFinish,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CyberSurfaceDark
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text("잠금이 작동하려면 권한이 필요합니다", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "설정을 열었다 돌아오면 자동으로 상태가 갱신됩니다. 필수 ${requiredMissing.size}개가 남아 있습니다.",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = if (requiredMissing.isEmpty()) NeonGreen else NeonCyan,
                trackColor = CyberBorder,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text("$grantedCount / ${steps.size} 완료", color = TextSecondary, fontSize = 11.sp)

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = bodyMaxHeight).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                steps.forEach { step ->
                    PermissionStepCard(step = step, onLaunch = { onLaunchPermission(step) })
                }
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (requiredMissing.isEmpty()) NeonGreen else Color(0xFF2A2F3A),
                    contentColor = if (requiredMissing.isEmpty()) Color.Black else TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (requiredMissing.isEmpty()) "설정 완료하고 시작하기" else "나중에 하기", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun PermissionStepCard(step: PermissionStep, onLaunch: () -> Unit) {
    val accent = when {
        step.granted -> NeonGreen
        step.required -> NeonRed
        else -> NeonAmber
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberCardDark, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(22.dp).background(accent.copy(alpha = 0.18f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (step.granted) "✓" else "!", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(step.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = when {
                            step.granted -> "완료"
                            step.required -> "필수"
                            else -> "권장"
                        },
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!step.granted) {
                    OutlinedButton(
                        onClick = onLaunch,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                        modifier = Modifier.height(38.dp)
                    ) { Text(step.actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(step.why, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}