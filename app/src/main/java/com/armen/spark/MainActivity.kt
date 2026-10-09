package com.armen.spark

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.armen.spark.notify.Notifications
import com.armen.spark.ui.nav.SparkNavHost
import com.armen.spark.ui.theme.SparkTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val pendingIdeaId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingIdeaId.value = intent.ideaId()
        val container = (application as SparkApp).container
        setContent {
            SparkTheme {
                SparkNavHost(container, pendingIdeaId, onPendingIdeaConsumed = { pendingIdeaId.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.ideaId()?.let { pendingIdeaId.value = it }
    }

    private fun Intent.ideaId(): Long? = getLongExtra(Notifications.EXTRA_IDEA_ID, -1L).takeIf { it > 0 }
}
