package com.asim.splitmate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.asim.splitmate.core.navigation.ExpenseMateNavHost
import com.asim.splitmate.core.ui.theme.ExpenseMateTheme

class MainActivity : ComponentActivity() {

    private var pendingGroupId by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingGroupId = extractGroupId(intent)

        requestNotificationPermission()

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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
