package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object InstalledAppsManager {

    /**
     * Checks if the app has permission to draw over other apps (SYSTEM_ALERT_WINDOW).
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Creates an Intent to open the "Draw over other apps" settings page.
     */
    fun getOverlayPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    /**
     * Scans and loads real installed applications with launcher intents from the user's device.
     */
    suspend fun loadInstalledApps(
        context: Context,
        currentlyLockedPackages: Set<String>
    ): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val appList = mutableListOf<AppItem>()
        val selfPackage = context.packageName
        val foundPackages = mutableSetOf<String>()

        // 1. Primary: Query all activities with MAIN + LAUNCHER intent
        try {
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA)
            }

            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo?.packageName ?: continue
                if (foundPackages.contains(pkgName)) continue

                val appInfo = try {
                    resolveInfo.activityInfo?.applicationInfo ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(0L))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(pkgName, 0)
                    }
                } catch (_: Throwable) {
                    continue
                }

                val appName = try {
                    resolveInfo.loadLabel(pm).toString().ifBlank {
                        pm.getApplicationLabel(appInfo).toString()
                    }
                } catch (_: Throwable) {
                    pkgName
                }

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val icon = try {
                    resolveInfo.loadIcon(pm) ?: pm.getApplicationIcon(appInfo)
                } catch (_: Throwable) {
                    null
                }

                foundPackages.add(pkgName)
                val isSelf = (pkgName == selfPackage)
                appList.add(
                    AppItem(
                        id = pkgName,
                        name = if (isSelf) "🛡️ App Lock (본 앱 자체 보호)" else appName,
                        packageName = pkgName,
                        category = if (isSelf) "보호 잠금 관리자" else if (isSystem) "시스템 앱" else "사용자 설치 앱",
                        isLocked = currentlyLockedPackages.contains(pkgName),
                        isSystemApp = isSystem,
                        iconDrawable = icon
                    )
                )
            }
        } catch (_: Throwable) {
            // continue to fallback
        }

        // 2. Secondary: Query getInstalledApplications
        try {
            val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }

            for (appInfo in installedApps) {
                val pkgName = appInfo.packageName ?: continue
                if (foundPackages.contains(pkgName)) continue

                val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                // Include if launchable OR user installed app
                if (launchIntent != null || !isSystem || pkgName == selfPackage) {
                    val appName = try {
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Throwable) {
                        pkgName
                    }
                    val icon = try {
                        pm.getApplicationIcon(appInfo)
                    } catch (_: Throwable) {
                        null
                    }
                    foundPackages.add(pkgName)
                    val isSelf = (pkgName == selfPackage)
                    appList.add(
                        AppItem(
                            id = pkgName,
                            name = if (isSelf) "🛡️ App Lock (본 앱 자체 보호)" else appName,
                            packageName = pkgName,
                            category = if (isSelf) "보호 잠금 관리자" else if (isSystem) "시스템 앱" else "사용자 설치 앱",
                            isLocked = currentlyLockedPackages.contains(pkgName),
                            isSystemApp = isSystem,
                            iconDrawable = icon
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            // continue
        }

        // 3. Enrich with popular essential apps (KakaoTalk, YouTube, Instagram, Toss, KakaoBank, etc.)
        val defaultApps = listOf(
            AppItem(
                id = selfPackage,
                name = "🛡️ App Lock (본 앱 자체 보호)",
                packageName = selfPackage,
                category = "보호 잠금 관리자",
                isLocked = currentlyLockedPackages.contains(selfPackage),
                isSystemApp = false
            ),
                AppItem(
                    id = "com.kakao.talk",
                    name = "카카오톡 (KakaoTalk)",
                    packageName = "com.kakao.talk",
                    category = "메신저 / SNS",
                    isLocked = currentlyLockedPackages.contains("com.kakao.talk"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.google.android.youtube",
                    name = "YouTube",
                    packageName = "com.google.android.youtube",
                    category = "동영상 / 미디어",
                    isLocked = currentlyLockedPackages.contains("com.google.android.youtube"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.instagram.android",
                    name = "Instagram",
                    packageName = "com.instagram.android",
                    category = "사진 / SNS",
                    isLocked = currentlyLockedPackages.contains("com.instagram.android"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.nhn.android.search",
                    name = "네이버 (NAVER)",
                    packageName = "com.nhn.android.search",
                    category = "포털 / 검색",
                    isLocked = currentlyLockedPackages.contains("com.nhn.android.search"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.android.chrome",
                    name = "Chrome 브라우저",
                    packageName = "com.android.chrome",
                    category = "인터넷",
                    isLocked = currentlyLockedPackages.contains("com.android.chrome"),
                    isSystemApp = true
                ),
                AppItem(
                    id = "com.google.android.apps.photos",
                    name = "갤러리 / 사진 (Photos)",
                    packageName = "com.google.android.apps.photos",
                    category = "사진 보관함",
                    isLocked = currentlyLockedPackages.contains("com.google.android.apps.photos"),
                    isSystemApp = true
                ),
                AppItem(
                    id = "viva.republica.toss",
                    name = "토스 (Toss 금융)",
                    packageName = "viva.republica.toss",
                    category = "금융 / 보안",
                    isLocked = currentlyLockedPackages.contains("viva.republica.toss"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.kakaobank.channel",
                    name = "카카오뱅크 (KakaoBank)",
                    packageName = "com.kakaobank.channel",
                    category = "금융 / 보안",
                    isLocked = currentlyLockedPackages.contains("com.kakaobank.channel"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.towneers.member",
                    name = "당근 (당근마켓)",
                    packageName = "com.towneers.member",
                    category = "쇼핑 / 거래",
                    isLocked = currentlyLockedPackages.contains("com.towneers.member"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.sample.baemin",
                    name = "배달의민족 (Baemin)",
                    packageName = "com.sample.baemin",
                    category = "생활 / 음식",
                    isLocked = currentlyLockedPackages.contains("com.sample.baemin"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.coupang.mobile",
                    name = "쿠팡 (Coupang)",
                    packageName = "com.coupang.mobile",
                    category = "쇼핑",
                    isLocked = currentlyLockedPackages.contains("com.coupang.mobile"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.netflix.mediaclient",
                    name = "넷플릭스 (Netflix)",
                    packageName = "com.netflix.mediaclient",
                    category = "동영상 / 미디어",
                    isLocked = currentlyLockedPackages.contains("com.netflix.mediaclient"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "org.telegram.messenger",
                    name = "텔레그램 (Telegram)",
                    packageName = "org.telegram.messenger",
                    category = "메신저 / SNS",
                    isLocked = currentlyLockedPackages.contains("org.telegram.messenger"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.discord",
                    name = "디스코드 (Discord)",
                    packageName = "com.discord",
                    category = "메신저 / 커뮤니티",
                    isLocked = currentlyLockedPackages.contains("com.discord"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.zhiliaoapp.musically",
                    name = "틱톡 (TikTok)",
                    packageName = "com.zhiliaoapp.musically",
                    category = "동영상 / SNS",
                    isLocked = currentlyLockedPackages.contains("com.zhiliaoapp.musically"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.twitter.android",
                    name = "X (구 트위터)",
                    packageName = "com.twitter.android",
                    category = "SNS / 소통",
                    isLocked = currentlyLockedPackages.contains("com.twitter.android"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.facebook.katana",
                    name = "페이스북 (Facebook)",
                    packageName = "com.facebook.katana",
                    category = "SNS / 소통",
                    isLocked = currentlyLockedPackages.contains("com.facebook.katana"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.android.settings",
                    name = "시스템 설정 (Settings)",
                    packageName = "com.android.settings",
                    category = "시스템 앱",
                    isLocked = currentlyLockedPackages.contains("com.android.settings"),
                    isSystemApp = true
                ),
                AppItem(
                    id = "com.android.camera",
                    name = "카메라 (Camera)",
                    packageName = "com.android.camera",
                    category = "시스템 앱",
                    isLocked = currentlyLockedPackages.contains("com.android.camera"),
                    isSystemApp = true
                ),
                AppItem(
                    id = "com.android.contacts",
                    name = "연락처 / 전화 (Contacts)",
                    packageName = "com.android.contacts",
                    category = "연락처 / 통화",
                    isLocked = currentlyLockedPackages.contains("com.android.contacts"),
                    isSystemApp = true
                ),
                AppItem(
                    id = "com.google.android.apps.messaging",
                    name = "메시지 (SMS)",
                    packageName = "com.google.android.apps.messaging",
                    category = "메시지",
                    isLocked = currentlyLockedPackages.contains("com.google.android.apps.messaging"),
                    isSystemApp = true
                ),
                AppItem(
                    id = "com.android.vending",
                    name = "Google Play 스토어 (앱 설치 및 결제)",
                    packageName = "com.android.vending",
                    category = "시스템 앱",
                    isLocked = currentlyLockedPackages.contains("com.android.vending"),
                    isSystemApp = true
                ),
                AppItem(
                    id = "com.google.android.apps.nbu.files",
                    name = "파일 관리자 (Files)",
                    packageName = "com.google.android.apps.nbu.files",
                    category = "시스템 도구",
                    isLocked = currentlyLockedPackages.contains("com.google.android.apps.nbu.files"),
                    isSystemApp = true
                ),
                AppItem(
                    id = "com.nhn.android.webtoon",
                    name = "네이버 웹툰 (Webtoon)",
                    packageName = "com.nhn.android.webtoon",
                    category = "엔터테인먼트",
                    isLocked = currentlyLockedPackages.contains("com.nhn.android.webtoon"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "notion.id",
                    name = "노션 (Notion 메모)",
                    packageName = "notion.id",
                    category = "생산성 / 업무",
                    isLocked = currentlyLockedPackages.contains("notion.id"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.kbstar.kbbank",
                    name = "KB국민은행 (KB스타뱅킹)",
                    packageName = "com.kbstar.kbbank",
                    category = "금융 / 보안",
                    isLocked = currentlyLockedPackages.contains("com.kbstar.kbbank"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.shinhan.sbanking",
                    name = "신한 SOL뱅크",
                    packageName = "com.shinhan.sbanking",
                    category = "금융 / 보안",
                    isLocked = currentlyLockedPackages.contains("com.shinhan.sbanking"),
                    isSystemApp = false
                ),
                AppItem(
                    id = "com.iloen.melon",
                    name = "멜론 (Melon 음악)",
                    packageName = "com.iloen.melon",
                    category = "음악 / 미디어",
                    isLocked = currentlyLockedPackages.contains("com.iloen.melon"),
                    isSystemApp = false
                )
        )
        for (app in defaultApps) {
            if (!foundPackages.contains(app.packageName)) {
                appList.add(app)
                foundPackages.add(app.packageName)
            }
        }

        // Sort: Self-App FIRST, then user installed apps, then alphabetically
        return@withContext appList.sortedWith(
            compareBy<AppItem> { it.packageName != selfPackage }
                .thenBy { it.isSystemApp }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Launch target app directly (simulates passing through lock screen).
     */
    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
