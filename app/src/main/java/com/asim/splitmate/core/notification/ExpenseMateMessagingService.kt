package com.asim.splitmate.core.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ExpenseMateMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_Service", "New FCM registration token received: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_Service", "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "SplitMate Expense Update"

        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "New activity in your group."

        val paidByUserId = remoteMessage.data["paidByUserId"] ?: ""
        val createdBy = remoteMessage.data["createdBy"] ?: ""
        val currentUid = com.asim.splitmate.core.firebase.FirebaseHelper.currentUserId ?: ""

        if (currentUid.isNotBlank() && (paidByUserId == currentUid || createdBy == currentUid)) {
            Log.d("FCM_Service", "Ignoring self notification for user $currentUid")
            return
        }

        val groupName = remoteMessage.data["groupName"] ?: "Group"
        val expenseTitle = remoteMessage.data["expenseTitle"] ?: "Expense"
        val amountStr = remoteMessage.data["amount"] ?: "0.0"
        val currencySymbol = remoteMessage.data["currencySymbol"] ?: "$"
        val paidByName = remoteMessage.data["paidByName"] ?: "A group member"

        val amount = amountStr.toDoubleOrNull() ?: 0.0

        NotificationHelper.showExpenseAddedNotification(
            context = applicationContext,
            groupName = groupName,
            expenseTitle = expenseTitle,
            amount = amount,
            currencySymbol = currencySymbol,
            paidByName = paidByName
        )
    }
}
