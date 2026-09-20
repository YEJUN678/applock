package com.example.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.LockActivity
import com.example.R
import com.example.util.AppLockPreferences

/** Immediately ends temporary unlocks and opens the protected lock screen. */
class InstantLockQuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = getString(R.string.instant_lock_tile_label)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        AppLockPreferences.resetAllTemporaryUnlocks()
        // Collapsing first avoids leaving the sensitive shade controls visible behind the lock UI.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            startActivityAndCollapse(android.app.PendingIntent.getActivity(
                this,
                0,
                android.content.Intent(this, LockActivity::class.java).apply {
                    putExtra(LockActivity.EXTRA_PACKAGE_NAME, packageName)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            ))
        } else {
            LockActivity.start(this, packageName)
        }
    }
}
