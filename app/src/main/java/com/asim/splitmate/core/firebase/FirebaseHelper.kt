package com.asim.splitmate.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object FirebaseHelper {
    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }

    val database: FirebaseDatabase?
        get() = try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            null
        }

    val currentUserId: String?
        get() = auth?.currentUser?.uid
}
