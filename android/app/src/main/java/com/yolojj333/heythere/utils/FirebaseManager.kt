package com.yolojj333.heythere.utils

import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.yolojj333.heythere.models.User

object FirebaseManager {

    // Helper to always get the explicitly named database
    private fun getExplicitDatabase(): FirebaseFirestore {
        return FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "users")
    }

    /**
     * Saves the entire User object to Firestore automatically.
     */
    fun saveUserProfile(user: User, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val db = getExplicitDatabase()

        db.collection("users").document(user.userId)
            .set(user)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Retrieves the User object from Firestore.
     */
    fun getUserProfile(uid: String, onResult: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        val db = getExplicitDatabase()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    onResult(user)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Listens to all active users for the map.
     */
    fun listenToAllUsers(onResult: (List<User>) -> Unit, onFailure: (Exception) -> Unit) {
        val db = getExplicitDatabase()

        db.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null) {
                onFailure(e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                onResult(users)
            }
        }
    }

    /**
     * Uploads the image to Cloud Storage.
     */
    fun uploadProfileImage(uri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onFailure(Exception("User is not authenticated."))
            return
        }

        val storage = FirebaseStorage.getInstance()
        val imageRef = storage.reference.child("profile_images/$userId.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onSuccess(downloadUri.toString())
                    }
                    .addOnFailureListener { e ->
                        onFailure(Exception("Failed to get download URL: ${e.message}"))
                    }
            }
            .addOnFailureListener { e ->
                val errorMessage = if (e is StorageException) {
                    when (e.errorCode) {
                        StorageException.ERROR_NOT_AUTHORIZED -> "Permission denied. Ensure the file is an image and under 5MB."
                        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> "Network timeout. Check your connection."
                        else -> e.message ?: "Unknown storage error."
                    }
                } else {
                    e.message ?: "Upload failed."
                }
                onFailure(Exception(errorMessage))
            }
    }

    /**
     * Deletes the physical image file from Cloud Storage.
     */
    fun deleteProfileImage(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onFailure(Exception("User is not authenticated."))
            return
        }

        val storage = FirebaseStorage.getInstance()
        val imageRef = storage.reference.child("profile_images/$userId.jpg")

        imageRef.delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                if (e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                    onSuccess()
                } else {
                    onFailure(e)
                }
            }
    }
}