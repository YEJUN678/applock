package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.BackgroundTheme
import com.example.ui.theme.CyberBgDark

@Composable
fun AppLockBackground(
    theme: BackgroundTheme,
    customImageUri: String? = null,
    dimAmount: Float = 0.82f,
    blurAmount: Float = 0f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 블러는 API 31+ 에서만 실제 적용되고, 그 미만에서는 scrim 강도로 대신 처리한다.
    val blurDp = (blurAmount * 24f).dp
    val scrimBoost = blurAmount * 0.22f
    Box(modifier = modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().blur(if (blurAmount > 0.01f) blurDp else 0.dp)) {
            if (!customImageUri.isNullOrBlank()) {
                AsyncImage(
                    model = customImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else when (theme) {
                BackgroundTheme.CYBER_WALLPAPER -> {
                    Image(
                        painter = painterResource(id = R.drawable.bg_cyber_lock),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xCC090D16),
                                        Color(0xE60D1527),
                                        Color(0xF2090D16)
                                    )
                                )
                            )
                    )
                }
                BackgroundTheme.DEEP_SPACE -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1E1B4B),
                                    Color(0xFF0F172A),
                                    Color(0xFF020617)
                                )
                            )
                        )
                )
                BackgroundTheme.NEON_NIGHT -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2E0854),
                                    Color(0xFF091E3A),
                                    Color(0xFF050510)
                                )
                            )
                        )
                )
                BackgroundTheme.MINIMAL_STEALTH -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBgDark)
                )
            }
        }

        // 흐림 강도에 따라 읽기 성능을 위한 어두운 막을 덧댄다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (dimAmount * 0.35f + scrimBoost).coerceIn(0f, 0.95f)))
        )

        content()
    }
}