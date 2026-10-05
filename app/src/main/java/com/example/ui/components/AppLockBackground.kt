package com.example.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.BackgroundTheme
import com.example.ui.theme.CyberBgDark
import kotlinx.coroutines.delay

/**
 * 잠금 화면 배경.
 *
 * 한 장만 쓸 수도 있고, 여러 장을 등록해 지정한 간격으로 자동 전환할 수도 있다.
 * 전환은 페이드로 처리해 One UI 라이브 배경처럼 갑자기 바뀌지 않게 한다.
 */
@Composable
fun AppLockBackground(
    theme: BackgroundTheme,
    customImageUri: String? = null,
    dimAmount: Float = 0.82f,
    blurAmount: Float = 0f,
    galleryUris: List<String> = emptyList(),
    autoRotate: Boolean = false,
    rotateSeconds: Int = 30,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 단일 이미지 파라미터와 갤러리를 합쳐 하나의 목록으로 다룬다.
    val images = remember(customImageUri, galleryUris) {
        buildList {
            if (!customImageUri.isNullOrBlank()) add(customImageUri)
            galleryUris.forEach { if (it.isNotBlank() && it != customImageUri) add(it) }
        }
    }

    var index by remember(images) { mutableIntStateOf(0) }

    LaunchedEffect(images.size, autoRotate, rotateSeconds) {
        // 이미지가 한 장 이하면 전환할 필요가 없다.
        if (!autoRotate || images.size <= 1) return@LaunchedEffect
        while (true) {
            delay((rotateSeconds.coerceIn(5, 600) * 1000L))
            index = (index + 1) % images.size
        }
    }

    val fade = animateFloatAsState(
        targetValue = if (images.size > 1) 1f else 0f,
        animationSpec = tween(1200),
        label = "bgFade"
    )

    val blurDp = (blurAmount * 24f).dp
    val scrimBoost = blurAmount * 0.22f

    Box(modifier = modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().blur(if (blurAmount > 0.01f) blurDp else 0.dp)) {
            if (images.isNotEmpty()) {
                AsyncImage(
                    model = images[index.coerceIn(0, images.lastIndex)],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(fade.value)
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