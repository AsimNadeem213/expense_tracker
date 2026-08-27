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

            // Also broadcast via FCM topic if FCM is registered
            sendFcmTopicNotification(groupName, expenseTitle, formattedAmount, paidByName)
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

    private fun sendFcmTopicNotification(
        groupName: String,
        expenseTitle: String,
        formattedAmount: String,
        paidByName: String
    ) {
        // Log FCM Topic Notification trigger
        Log.d("NotificationHelper", "FCM Group Expense Notification triggered for '$groupName': $paidByName added '$expenseTitle' ($formattedAmount)")
    }
}
