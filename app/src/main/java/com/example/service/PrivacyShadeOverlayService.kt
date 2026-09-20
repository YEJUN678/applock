package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * System-wide Privacy Screen Filter (화면 엿보기 방지 가림막 서비스).
 * Runs on top of ALL apps via WindowManager TYPE_APPLICATION_OVERLAY.
 *
 * Provides a software privacy shade overlay.
 * - A software overlay cannot distinguish a viewer's angle; use a physical privacy film for
 *   side-view blocking while keeping the front view clear.
 * - The default is tuned for front-view readability, with an adjustable reading window.
 * - Includes a movable floating controller pill to adjust opacity, pattern, and color on-the-fly.
 */
class PrivacyShadeOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.example.action.START_PRIVACY_SHADE"
        const val ACTION_STOP = "com.example.action.STOP_PRIVACY_SHADE"
        const val CHANNEL_ID = "privacy_shade_channel"
        const val NOTIFICATION_ID = 2026

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, PrivacyShadeOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PrivacyShadeOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var shadeView: LouverPrivacyView? = null
    private var floatingPillView: FrameLayout? = null

    // Filter properties
    // In reading-window mode this only applies outside the clear window, so it can be strong
    // without reducing the owner's readability of the content being read.
    private var filterOpacity = 0.62f
    private var louverSpacingDp = 6f   // less visual noise for the owner
    private var filterColor = Color.BLACK
    private var isSpotlightEnabled = true
    private var spotlightYRatio = 0.45f
    private var spotlightHeightDp = 180f
    private var isControlsExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        isRunning = true

        if (shadeView == null) {
            setupShadeOverlay()
            setupFloatingController()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "엿보기 방지 가림막 서비스",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "다른 앱 위에 표시되는 프라이버시 사생활 보호 필터"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PrivacyShadeOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ 엿보기 방지 가림막 작동 중")
            .setContentText("화면 가림막 작동 중 · 측면 차단에는 물리 필름이 필요합니다")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "가림막 끄기", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupShadeOverlay() {
        val wm = windowManager ?: return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            // CRITICAL: FLAG_NOT_TOUCHABLE passes all taps through to KakaoTalk, YouTube, etc.
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val view = LouverPrivacyView(this)
        shadeView = view
        try {
            wm.addView(view, params)
        } catch (_: Exception) {}
    }

    private fun setupFloatingController() {
        val wm = windowManager ?: return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val pillParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 300
        }

        val container = FrameLayout(this)
        floatingPillView = container

        // Inflate or programmatically build dynamic controller
        rebuildControllerView(container, pillParams)

        try {
            wm.addView(container, pillParams)
        } catch (_: Exception) {}
    }

    private fun rebuildControllerView(container: FrameLayout, params: WindowManager.LayoutParams) {
        container.removeAllViews()

        if (!isControlsExpanded) {
            // Minimized Floating Pill Badge (can be dragged)
            val pill = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#E60F172A")) // Slate 900
                    setStroke(dpToPx(1), Color.parseColor("#00F0FF")) // Neon Cyan
                    cornerRadius = dpToPx(20).toFloat()
                }
            }

            val icon = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_view)
                setColorFilter(Color.parseColor("#00F0FF"))
                layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(18))
            }
            pill.addView(icon)

            val text = TextView(this).apply {
                text = " 사생활 보호"
                setTextColor(Color.WHITE)
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            pill.addView(text)

            // Touch & Drag listener for the floating pill
            pill.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isClick = true

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if (event == null) return false
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isClick = true
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                                isClick = false
                            }
                            params.x = initialX - dx
                            params.y = initialY + dy
                            try {
                                windowManager?.updateViewLayout(container, params)
                            } catch (_: Exception) {}
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isClick) {
                                isControlsExpanded = true
                                rebuildControllerView(container, params)
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            container.addView(pill)
        } else {
            // Expanded Quick Control Panel
            val panel = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#F20B0F19")) // Cyber Dark
                    setStroke(dpToPx(1), Color.parseColor("#00F0FF"))
                    cornerRadius = dpToPx(16).toFloat()
                }
                layoutParams = FrameLayout.LayoutParams(dpToPx(270), FrameLayout.LayoutParams.WRAP_CONTENT)
            }

            // Top Header: Title + Close panel button
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val title = TextView(this).apply {
                text = "🛡️ 엿보기 방지 세부 조절"
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            header.addView(title)

            val btnMinimize = TextView(this).apply {
                text = "접기 ▲"
                setTextColor(Color.parseColor("#00F0FF"))
                textSize = 11f
                setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
                setOnClickListener {
                    isControlsExpanded = false
                    rebuildControllerView(container, params)
                }
            }
            header.addView(btnMinimize)
            panel.addView(header)

            // Opacity Slider Label
            val opacityLabel = TextView(this).apply {
                text = "화면 가림 강도: ${(filterOpacity * 100).toInt()}%"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 11f
                setPadding(0, dpToPx(8), 0, dpToPx(2))
            }
            panel.addView(opacityLabel)

            // Opacity SeekBar
            val opacityBar = SeekBar(this).apply {
                max = 70 // 20% to 90%
                progress = ((filterOpacity - 0.20f) * 100).toInt()
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        filterOpacity = 0.20f + (progress / 100f)
                        opacityLabel.text = "화면 가림 강도: ${(filterOpacity * 100).toInt()}%"
                        shadeView?.invalidate()
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
            panel.addView(opacityBar)

            // Louver Slat Density Slider Label
            val densityLabel = TextView(this).apply {
                text = "화면 패턴 간격: ${louverSpacingDp.toInt()}dp"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 11f
                setPadding(0, dpToPx(6), 0, dpToPx(2))
            }
            panel.addView(densityLabel)

            val densityBar = SeekBar(this).apply {
                max = 6 // 2dp to 8dp
                progress = (louverSpacingDp - 2f).toInt()
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        louverSpacingDp = 2f + progress
                        densityLabel.text = "화면 패턴 간격: ${louverSpacingDp.toInt()}dp"
                        shadeView?.updateShader()
                        shadeView?.invalidate()
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
            panel.addView(densityBar)

            // Action Buttons Row: Spotlight Mode & Shut down
            val actionRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dpToPx(10)
                }
            }

            val btnSpotlight = TextView(this).apply {
                text = if (isSpotlightEnabled) "집중 시야창 켜짐 👁️" else "집중 시야창"
                setTextColor(if (isSpotlightEnabled) Color.parseColor("#00FF66") else Color.WHITE)
                textSize = 11f
                gravity = Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = dpToPx(8).toFloat()
                    setStroke(dpToPx(1), if (isSpotlightEnabled) Color.parseColor("#00FF66") else Color.GRAY)
                }
                setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dpToPx(6)
                }
                setOnClickListener {
                    isSpotlightEnabled = !isSpotlightEnabled
                    text = if (isSpotlightEnabled) "집중 시야창 켜짐 👁️" else "집중 시야창"
                    (background as? android.graphics.drawable.GradientDrawable)?.setStroke(
                        dpToPx(1),
                        if (isSpotlightEnabled) Color.parseColor("#00FF66") else Color.GRAY
                    )
                    setTextColor(if (isSpotlightEnabled) Color.parseColor("#00FF66") else Color.WHITE)
                    shadeView?.invalidate()
                }
            }
            actionRow.addView(btnSpotlight)

            val btnClose = TextView(this).apply {
                text = "가림막 종료 ❌"
                setTextColor(Color.parseColor("#FF3366"))
                textSize = 11f
                gravity = Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#331E293B"))
                    cornerRadius = dpToPx(8).toFloat()
                    setStroke(dpToPx(1), Color.parseColor("#FF3366"))
                }
                setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    stopSelf()
                }
            }
            actionRow.addView(btnClose)
            panel.addView(actionRow)

            container.addView(panel)
        }

        try {
            windowManager?.updateViewLayout(container, params)
        } catch (_: Exception) {}
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        val wm = windowManager
        if (wm != null) {
            try {
                if (shadeView != null) {
                    wm.removeView(shadeView)
                    shadeView = null
                }
            } catch (_: Exception) {}
            try {
                if (floatingPillView != null) {
                    wm.removeView(floatingPillView)
                    floatingPillView = null
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Custom View that paints micro-louver stripes and spotlight window over all apps.
     */
    inner class LouverPrivacyView(context: Context) : View(context) {
        private val basePaint = Paint()
        private val louverPaint = Paint()
        private var louverShader: BitmapShader? = null

        init {
            updateShader()
        }

        fun updateShader() {
            val spacingPx = (louverSpacingDp * resources.displayMetrics.density).toInt().coerceAtLeast(4)
            val bmp = Bitmap.createBitmap(spacingPx, 2, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint()
            // Slit 1: Dark high-absorption louver slat line (1.5px)
            p.color = Color.argb(180, 0, 0, 0)
            c.drawRect(0f, 0f, spacingPx * 0.45f, 2f, p)
            // Slit 2: Transparent viewing gap
            p.color = Color.TRANSPARENT
            c.drawRect(spacingPx * 0.45f, 0f, spacingPx.toFloat(), 2f, p)

            louverShader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            louverPaint.shader = louverShader
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            val alphaInt = (filterOpacity * 255).toInt().coerceIn(0, 255)

            if (!isSpotlightEnabled) {
                // A full-screen software shade affects every viewing angle equally.
                basePaint.color = Color.argb(alphaInt, Color.red(filterColor), Color.green(filterColor), Color.blue(filterColor))
                canvas.drawRect(0f, 0f, w, h, basePaint)

                // Micro-Louver stripes overlay
                louverPaint.alpha = (alphaInt * 0.85f).toInt()
                canvas.drawRect(0f, 0f, w, h, louverPaint)
            } else {
                // The central reading band stays completely clear while the surrounding screen
                // is strongly shaded, limiting the amount of information visible at a glance.
                val spotHeightPx = spotlightHeightDp * resources.displayMetrics.density
                val spotTop = (h * spotlightYRatio) - (spotHeightPx / 2f)
                val spotBottom = spotTop + spotHeightPx

                basePaint.color = Color.argb(alphaInt, 0, 0, 0)
                louverPaint.alpha = (alphaInt * 0.85f).toInt()

                // Top block
                if (spotTop > 0) {
                    canvas.drawRect(0f, 0f, w, spotTop, basePaint)
                    canvas.drawRect(0f, 0f, w, spotTop, louverPaint)
                }

                // Bottom block
                if (spotBottom < h) {
                    canvas.drawRect(0f, spotBottom, w, h, basePaint)
                    canvas.drawRect(0f, spotBottom, w, h, louverPaint)
                }

                // Subtle neon border around the spotlight reading window
                val borderPaint = Paint().apply {
                    color = Color.parseColor("#8000F0FF")
                    strokeWidth = 2f * resources.displayMetrics.density
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(0f, spotTop, w, spotBottom, borderPaint)
            }
        }
    }
}
