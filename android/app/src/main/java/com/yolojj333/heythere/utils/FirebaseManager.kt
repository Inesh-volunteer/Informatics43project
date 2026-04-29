package com.yolojj333.heythere.utils

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.yolojj333.heythere.models.User

object FirebaseManager {

    private val db get() = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "users")
    private val usersCollection get() = db.collection("users")

    fun saveUserProfile(user: User, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (user.userId.isBlank()) {
            onFailure(Exception("User ID is missing! Try logging out and back in."))
            return
        }

        try {
            usersCollection.document(user.userId)
                .set(user)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { exception -> onFailure(exception) }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    fun getUserProfile(uid: String, onResult: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        if (uid.isBlank()) {
            onFailure(Exception("UID is blank"))
            return
        }

        try {
            usersCollection.document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val user = document.toObject(User::class.java)
                        onResult(user)
                    } else {
                        onResult(null)
                    }
                }
                .addOnFailureListener { exception -> onFailure(exception) }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // NEW: Real-time listener that automatically fetches all users and listens for changes
    fun listenToAllUsers(onResult: (List<User>) -> Unit, onFailure: (Exception) -> Unit) {
        try {
            usersCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                    onResult(users)
                }
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}