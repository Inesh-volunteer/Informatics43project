package com.yolojj333.heythere.utils

import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    /**
     * Uploads a local image URI to Firebase Cloud Storage and returns the public download URL.
     */
    fun uploadProfileImage(uri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onFailure(Exception("User is not authenticated."))
            return
        }

        val storage = FirebaseStorage.getInstance()
        // We use the userId as the filename so it naturally overwrites their old picture
        val imageRef = storage.reference.child("profile_images/$userId.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                // The upload finished, now we must request the public URL
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onSuccess(downloadUri.toString())
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}