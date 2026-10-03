package com.asim.splitmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.asim.splitmate.core.navigation.ExpenseMateNavHost
import com.asim.splitmate.core.ui.theme.ExpenseMateTheme

class MainActivity : ComponentActivity() {

    private var pendingGroupId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingGroupId = extractGroupId(intent)

        setContent {
            ExpenseMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    ExpenseMateNavHost(
                        navController = navController,
                        pendingGroupId = pendingGroupId,
                        onGroupIdHandled = { pendingGroupId = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val gId = extractGroupId(intent)
        if (!gId.isNullOrBlank()) {
            pendingGroupId = gId
        }
    }

    private fun extractGroupId(intent: Intent?): String? {
        if (intent == null) return null
        return intent.getStringExtra("groupId") ?: intent.extras?.getString("groupId")
    }
}
