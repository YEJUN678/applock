package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.model.BackgroundTheme
import com.example.ui.theme.CyberBgDark

@Composable
fun AppLockBackground(
    theme: BackgroundTheme,
    customImageUri: String? = null,
    dimAmount: Float = 0.82f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (!customImageUri.isNullOrBlank()) {
            AsyncImage(
                model = customImageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = dimAmount * 0.7f), Color(0xFF0D1527).copy(alpha = dimAmount), Color.Black.copy(alpha = dimAmount)))
                )
            )
        } else when (theme) {
            BackgroundTheme.CYBER_WALLPAPER -> {
                Image(
                    painter = painterResource(id = R.drawable.bg_cyber_lock),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark overlay to ensure contrast
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
            BackgroundTheme.DEEP_SPACE -> {
                Box(
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
            }
            BackgroundTheme.NEON_NIGHT -> {
                Box(
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
            }
            BackgroundTheme.MINIMAL_STEALTH -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBgDark)
                )
            }
        }

        content()
    }
}
