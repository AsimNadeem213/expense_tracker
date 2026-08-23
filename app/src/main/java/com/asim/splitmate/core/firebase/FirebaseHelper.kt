package com.asim.splitmate.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object FirebaseHelper {

    private const val RTDB_URL =
        "https://authfirebase1-3b472-default-rtdb.firebaseio.com"

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val database: FirebaseDatabase
        get() = FirebaseDatabase.getInstance(RTDB_URL)

    val currentUserId: String?
        get() = auth.currentUser?.uid
}
