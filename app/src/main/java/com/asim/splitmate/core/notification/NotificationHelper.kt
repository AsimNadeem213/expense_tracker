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
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object NotificationHelper {

    const val CHANNEL_ID = "group_expense_channel"
    private const val CHANNEL_NAME = "Group Expense Updates"
    private const val CHANNEL_DESC = "Notifications when new expenses or settlements are added in your groups"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showExpenseAddedNotification(
        context: Context,
        groupName: String,
        expenseTitle: String,
        amount: Double,
        currencySymbol: String,
        paidByName: String
    ) {
        try {
            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "New Expense in $groupName"
            val formattedAmount = "$currencySymbol${String.format("%.2f", amount)}"
            val message = "$paidByName added '$expenseTitle' ($formattedAmount)"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$message in group '$groupName'"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to post notification: ${e.message}", e)
        }
    }

    fun subscribeToGroupTopic(groupId: String) {
        try {
            val cleanId = groupId.replace("[^a-zA-Z0-9-_.~%]", "_")
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
        try {
            val cleanId = groupId.replace("[^a-zA-Z0-9-_.~%]", "_")
            FirebaseMessaging.getInstance().unsubscribeFromTopic("group_$cleanId")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "FCM topic unsubscription error: ${e.message}")
        }
    }

    fun sendExpenseNotificationToGroup(
        groupId: String,
        groupName: String,
        expenseTitle: String,
        amount: Double,
        currencySymbol: String,
        paidByName: String,
        paidByUserId: String,
        context: Context? = null
    ) {
        if (context != null && !isNetworkAvailable(context)) {
            Log.d("NotificationHelper", "Offline: Skipping FCM push notification")
            return
        }

        val cleanId = groupId.replace("[^a-zA-Z0-9-_.~%]", "_")
        val topic = "/topics/group_$cleanId"
        val formattedAmount = "$currencySymbol${String.format("%.2f", amount)}"

        val title = "New Expense in $groupName"
        val body = "$paidByName added '$expenseTitle' ($formattedAmount)"

        Log.d("NotificationHelper", "Triggering FCM Push to $topic for '$groupName'")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("to", topic)
                    put("priority", "high")
                    put("notification", JSONObject().apply {
                        put("title", title)
                        put("body", body)
                        put("sound", "default")
                    })
                    put("data", JSONObject().apply {
                        put("groupId", groupId)
                        put("groupName", groupName)
                        put("expenseTitle", expenseTitle)
                        put("amount", amount.toString())
                        put("currencySymbol", currencySymbol)
                        put("paidByName", paidByName)
                        put("paidByUserId", paidByUserId)
                        put("createdBy", paidByUserId)
                    })
                }

                val url = URL("https://fcm.googleapis.com/fcm/send")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "key=AIzaSyBZuPha4cQAeKItbOcZ3JfEkqlKP-SlZ_s")
                conn.doOutput = true

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(json.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                Log.d("NotificationHelper", "FCM Topic push response code: $responseCode")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error sending FCM topic push: ${e.message}", e)
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
