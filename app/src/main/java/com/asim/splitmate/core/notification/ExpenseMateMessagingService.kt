package com.asim.splitmate.core.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ExpenseMateMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_Service", "New FCM registration token received: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val createdBy = remoteMessage.data["createdBy"]
        val currentFirebaseUid = FirebaseAuth.getInstance().currentUser?.uid

        // Self Notification check:
        // If the current user creates the expense, do NOT show a notification to that same user.
        if (currentFirebaseUid != null && currentFirebaseUid == createdBy) {
            Log.d("FCM_Service", "Skipping self notification for creator: $currentFirebaseUid")
            return
        }

        val groupId = remoteMessage.data["groupId"] ?: ""
        NotificationHelper.showExpenseAddedNotification(
            context = applicationContext,
            groupId = groupId
        )
    }
}
