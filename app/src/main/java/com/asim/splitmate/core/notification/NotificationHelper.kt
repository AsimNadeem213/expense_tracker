package com.asim.splitmate.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.asim.splitmate.MainActivity
import com.asim.splitmate.R
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {

    const val CHANNEL_ID = "group_expense_channel_v3"
    private const val CHANNEL_NAME = "Group Expense Updates"
    private const val CHANNEL_DESC = "Notifications when new expenses are added in your groups"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showExpenseAddedNotification(
        context: Context,
        groupId: String
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted. Cannot post notification.")
                    return
                }
            }

            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("groupId", groupId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                groupId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "New Expense Added"
            val body = "A new expense was added to your group."

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(groupId.hashCode(), notification)
            Log.d("NotificationHelper", "Displayed notification: '$title' for group: $groupId")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to post notification: ${e.message}", e)
        }
    }

    fun subscribeToGroupTopic(groupId: String) {
        if (groupId.isBlank()) return
        try {
            val cleanId = groupId.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
            FirebaseMessaging.getInstance().subscribeToTopic("group_$cleanId")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("NotificationHelper", "Subscribed to FCM topic: group_$cleanId")
                    } else {
                        Log.e("NotificationHelper", "Failed to subscribe to FCM topic", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "FCM topic subscription error: ${e.message}")
        }
    }

    fun unsubscribeFromGroupTopic(groupId: String) {
        if (groupId.isBlank()) return
        try {
            val cleanId = groupId.replace(Regex("[^a-zA-Z0-9-_.~%]"), "_")
            FirebaseMessaging.getInstance().unsubscribeFromTopic("group_$cleanId")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "FCM topic unsubscription error: ${e.message}")
        }
    }

    fun sendExpenseNotificationToGroup(
        groupId: String,
        expenseId: String,
        createdBy: String,
        context: Context? = null
    ) {
        if (context != null && !isNetworkAvailable(context)) {
            Log.d("NotificationHelper", "Offline: Skipping FCM push notification")
            return
        }

        val title = "New Expense Added"
        val body = "A new expense was added to your group."

        CoroutineScope(Dispatchers.IO).launch {
            // Write notification event to Firebase Realtime Database
            try {
                com.asim.splitmate.core.firebase.FirebaseHelper.database?.getReference("groups")
                    ?.child(groupId)?.child("lastNotification")?.setValue(
                        mapOf(
                            "type" to "expense_added",
                            "groupId" to groupId,
                            "expenseId" to expenseId,
                            "createdBy" to createdBy,
                            "title" to title,
                            "body" to body,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                Log.d("NotificationHelper", "Logged expense notification to RTDB for group: $groupId")
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Failed to log notification to RTDB: ${e.message}")
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                     capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                     capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (_: Exception) {
            false
        }
    }
}
