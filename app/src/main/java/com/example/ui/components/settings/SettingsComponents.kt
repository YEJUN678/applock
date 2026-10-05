package com.example.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OneUi
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** One UI 계열 설정 화면 재사용 컴포넌트 모음. */

/** 큰 제목 + 부제 (화면 상단 헤더). */
@Composable
fun SettingsScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OneUi.RowSurface)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("‹", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = TextSecondary, fontSize = 13.sp)
            }
        }
        trailing?.invoke()
    }
}

/** 섹션 라벨 (카드 위쪽 작은 제목). */
@Composable
fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

/** One UI 카드: 둥근 24dp + 여백. */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    accent: Color = OneUi.Card,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(accent)
            .padding(OneUi.CardPadding),
        content = content
    )
}

/** 카드 안의 행 단위 컨테이너. 인접 행 사이는 얇은 구분선으로 나눈다. */
@Composable
fun SettingsRowGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        content = content
    )
}

/** 왼쪽 아이콘 타일 + 가운데 텍스트 + 오른쪽 콘텐츠. */
@Composable
fun SettingsRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    tint: Color = OneUi.AccentTint,
    value: String? = null,
    showChevron: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null && enabled) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = OneUi.RowMinHeight).clip(RoundedCornerShape(OneUi.RowRadius))
            .then(if (onClick != null && enabled) Modifier.background(OneUi.RowSurface) else Modifier)
            .then(clickModifier)
            .padding(OneUi.RowPadding)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(OneUi.IconTile).clip(RoundedCornerShape(13.dp)).background(OneUi.tintAlpha(tint)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(OneUi.IconGlyph)) }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) TextPrimary else TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        if (!value.isNullOrBlank()) {
            Text(value, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
        }
        trailing?.invoke()
        if (showChevron && onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

/** 스위치가 붙은 행. */
@Composable
fun SettingsToggleRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    tint: Color = OneUi.AccentTint,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        tint = tint,
        enabled = enabled,
        onClick = { if (enabled) onCheckedChange(!checked) },
        trailing = {
            // Row 자체가 클릭 가능하므로 스위치는 표시 전용으로 쓴다(중복 토글 방지).
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    )
}

/** 허브 화면의 섹션 진입 카드. */
@Composable
fun SettingsHubCard(
    icon: ImageVector,
    title: String,
    description: String,
    tint: Color,
    badge: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.CardSurface)
            .clickable(onClick = onClick)
            .padding(OneUi.CardPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(OneUi.tintAlpha(tint)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (!badge.isNullOrBlank()) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(OneUi.tintAlpha(tint)).padding(horizontal = 8.dp, vertical = 3.dp)
                ) { Text(badge, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(description, color = TextSecondary, fontSize = 12.sp)
    }
}

/** 상태 요약 배지 (권한 경고 등). */
@Composable
fun SettingsStatusBanner(
    tint: Color,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.tintAlpha(tint, 0.14f))
            .padding(OneUi.CardPadding)
    ) {
        Text(title, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(description, color = TextSecondary, fontSize = 12.sp)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OneUi.tintAlpha(tint, 0.22f))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) { Text(actionLabel, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

/** 카드 안에서 세로로 쌓을 때 쓰는 간격 헬퍼. */
@Composable
fun ColumnScope.SettingsStack(spacing: Int = 8) {
    Spacer(Modifier.height(0.dp))
}

/** 여러 값 중 하나를 고르는 시트 (잠금 방식·시간·민감도 등). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsChoiceSheet(
    title: String,
    subtitle: String? = null,
    options: List<Pair<T, String>>, // (값, 부가 설명)
    selectedIndex: Int,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = com.example.ui.theme.CyberSurfaceDark
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(title, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            options.forEachIndexed { index, (value, description) ->
                val isSelected = index == selectedIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(OneUi.RowRadius))
                        .background(if (isSelected) OneUi.tintAlpha(OneUi.AccentTint, 0.18f) else OneUi.RowSurface)
                        .clickable { onSelect(value) }
                        .padding(16.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(value.toString(), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        if (description.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(description, color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(OneUi.AccentTint)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** 값 하나만 보여주는 정보 행 (읽기 전용). */
@Composable
fun SettingsInfoRow(
    label: String,
    value: String,
    tint: Color = OneUi.AccentTint
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** 시트 안에서 여러 정보를 감싸는 카드. */
@Composable
fun SettingsSheetCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUi.CardRadius))
            .background(OneUi.RowSurface)
            .padding(vertical = 6.dp),
        content = {
            if (title != null) {
                Text(
                    title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp)
                )
            }
            content()
        }
    )
}