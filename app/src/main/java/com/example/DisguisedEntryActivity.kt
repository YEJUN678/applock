package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Non-launcher target kept enabled so a disguised launcher alias can safely open the app. */
class DisguisedEntryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); startActivity(Intent(this, MainActivity::class.java)); finish() }
}
