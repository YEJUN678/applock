package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.ui.theme.CyberCardDark
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** 목록 정렬 기준. */
enum class AppSortOrder(val title: String) {
    NAME_ASC("이름순"),
    LOCKED_FIRST("잠긴 앱 먼저"),
    UNLOCKED_FIRST("안 잠긴 앱 먼저"),
    CATEGORY("분류순")
}

/** 목록 보기 방식. */
enum class AppViewMode { LIST, GRID }

/**
 * 목록 상단 제어 바: 개수 · 보기 전환 · 정렬 · 카테고리 필터.
 * One UI 톤의 칩 + 아이콘 버튼으로 구성한다.
 */
@Composable
fun AppListControlBar(
    title: String,
    count: Int,
    viewMode: AppViewMode,
    onViewModeChange: (AppViewMode) -> Unit,
    sortOrder: AppSortOrder,
    onSortOrderChange: (AppSortOrder) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    onBatchSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$title ($count)",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            SmallIconButton(
                icon = if (viewMode == AppViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                contentDescription = "보기 전환",
                active = viewMode == AppViewMode.GRID
            ) { onViewModeChange(if (viewMode == AppViewMode.LIST) AppViewMode.GRID else AppViewMode.LIST) }
            Spacer(Modifier.width(6.dp))
            SmallIconButton(
                icon = Icons.Default.Sort,
                contentDescription = "정렬",
                active = sortOrder != AppSortOrder.NAME_ASC
            ) {
                val next = AppSortOrder.entries[(sortOrder.ordinal + 1) % AppSortOrder.entries.size]
                onSortOrderChange(next)
            }
        }

        // 카테고리 필터 (One UI 칩)
        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    CategoryChip("전체", selectedCategory == null) { onCategoryChange(null) }
                }
                items(categories.size) { index ->
                    val category = categories[index]
                    CategoryChip(category, selectedCategory == category) { onCategoryChange(category) }
                }
            }
        }
    }
}

@Composable
private fun SmallIconButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) NeonCyan.copy(alpha = 0.18f) else CyberCardDark)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (active) NeonCyan else TextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) Color.Black else TextSecondary,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) NeonCyan else CyberCardDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/**
 * 그리드용 앱 카드. 아이콘을 크게突出하고 잠금 배지를 겹쳐 표시한다.
 */
@Composable
fun AppGridCard(
    app: AppItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onToggleLock: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isSelected -> NeonCyan
        app.isLocked -> NeonCyan.copy(alpha = 0.45f)
        else -> CyberBorder
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CyberCardDark)
            .clickable {
                if (isSelectionMode) onSelectToggle() else onToggleLock(!app.isLocked)
            }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF20293C)),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = androidx.compose.runtime.remember(app.iconDrawable) {
                    app.iconDrawable?.let { drawableToBitmapOrNull(it) }
                }
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = app.name,
                        modifier = Modifier.size(38.dp)
                    )
                } else {
                    Icon(Icons.Rounded.Android, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(26.dp))
                }
            }
            if (app.isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(NeonCyan),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Lock, contentDescription = "잠김", tint = Color.Black, modifier = Modifier.size(12.dp)) }
            }
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) NeonCyan else Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) { if (isSelected) Text("✓", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.name,
            color = TextPrimary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = app.category,
            color = if (app.isLocked) NeonCyan.copy(alpha = 0.8f) else TextSecondary,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Drawable → Bitmap 변환이 실패하면 null 을 돌려준다 (아이콘이 이상한 앱 대비). */
private fun drawableToBitmapOrNull(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap? = try {
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    bitmap
} catch (_: Exception) {
    null
}

/** 정렬 + 카테고리 필터를 적용한다. */
fun List<AppItem>.applyListOptions(
    sortOrder: AppSortOrder,
    category: String?
): List<AppItem> {
    val filtered = if (category.isNullOrBlank() || category == "전체") this else filter { it.category == category }
    return when (sortOrder) {
        AppSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
        AppSortOrder.LOCKED_FIRST -> filtered.sortedWith(compareByDescending<AppItem> { it.isLocked }.thenBy { it.name.lowercase() })
        AppSortOrder.UNLOCKED_FIRST -> filtered.sortedWith(compareByDescending<AppItem> { !it.isLocked }.thenBy { it.name.lowercase() })
        AppSortOrder.CATEGORY -> filtered.sortedWith(compareBy({ it.category }, { it.name.lowercase() }))
    }
}