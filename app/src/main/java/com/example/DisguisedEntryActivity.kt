package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.components.CalculatorDisguiseLockView
import com.example.ui.theme.MyApplicationTheme

/** Non-launcher target kept enabled so a disguised launcher alias can safely open the app. */
class DisguisedEntryActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SHOW_CALCULATOR = "show_calculator_disguise"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_SHOW_CALCULATOR, false)) {
            setContent {
                MyApplicationTheme {
                    // Empty code keeps this as a genuine calculator surface; it cannot open App Lock.
                    CalculatorDisguiseLockView(targetCode = "", onCodeSubmitted = {}, modifier = Modifier.fillMaxSize())
                }
            }
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
