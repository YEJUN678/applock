package com.example.service

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings toggle for the system-wide privacy shade. */
class PrivacyQuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (PrivacyShadeOverlayService.isRunning) {
            PrivacyShadeOverlayService.stop(this)
        } else if (Settings.canDrawOverlays(this)) {
            PrivacyShadeOverlayService.start(this)
        } else {
            // The tile cannot draw over other apps until this one-time system permission is granted.
            startActivityAndCollapse(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        refreshTile()
    }

    private fun refreshTile() {
        qsTile?.apply {
            state = if (PrivacyShadeOverlayService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.privacy_tile_label)
            updateTile()
        }
    }

    companion object {
        fun requestRefresh(service: PrivacyQuickSettingsTileService) {
            // Kept for API symmetry if this service is ever bound directly.
            service.refreshTile()
        }

        fun requestListeningRefresh(context: android.content.Context) {
            try {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, PrivacyQuickSettingsTileService::class.java)
                )
            } catch (_: Exception) {
                // Quick Settings may be unavailable on some device builds.
            }
        }
    }
}
