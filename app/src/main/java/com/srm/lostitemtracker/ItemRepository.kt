package com.srm.lostitemtracker

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ItemRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun submitLostReport(
        context     : Context,
        itemName    : String,
        category    : String,
        description : String,
        location    : String,
        date        : String,
        photoUri    : Uri?
    ): Result<Unit> {
        return try {
            val uid      = auth.currentUser!!.uid
            val photoUrl = if (photoUri != null)
                CloudinaryManager.uploadImage(context, photoUri) else ""

            // Run ML Kit image labeling on the photo before saving
            // If no photo was selected, imageLabels will be an empty list
            val imageLabels = if (photoUri != null)
                ImageLabelingHelper.getLabels(context, photoUri) else emptyList()

            val item = LostItem(
                userId      = uid,
                itemName    = itemName,
                category    = category,
                description = description,
                location    = location,
                date        = date,
                photoUrl    = photoUrl,
                status      = "open",
                createdAt   = System.currentTimeMillis(),
                imageLabels = imageLabels
            )

            // Save document — Firestore generates the ID
            val docRef = db.collection("lost_items").add(item).await()

            // Write the real ID back into the document so it is never empty
            docRef.update("id", docRef.id).await()

            // Run matching engine with the correct ID set
            runMatchingForLost(item.copy(id = docRef.id))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitFoundReport(
        context     : Context,
        itemName    : String,
        category    : String,
        description : String,
        location    : String,
        date        : String,
        photoUri    : Uri?
    ): Result<Unit> {
        return try {
            val uid      = auth.currentUser!!.uid
            val photoUrl = if (photoUri != null)
                CloudinaryManager.uploadImage(context, photoUri) else ""

            val imageLabels = if (photoUri != null)
                ImageLabelingHelper.getLabels(context, photoUri) else emptyList()

            val item = FoundItem(
                userId      = uid,
                itemName    = itemName,
                category    = category,
                description = description,
                location    = location,
                date        = date,
                photoUrl    = photoUrl,
                status      = "open",
                createdAt   = System.currentTimeMillis(),
                imageLabels = imageLabels
            )

            val docRef = db.collection("found_items").add(item).await()
            docRef.update("id", docRef.id).await()
            runMatchingForFound(item.copy(id = docRef.id))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun runMatchingForLost(lost: LostItem) {
        val snap = db.collection("found_items")
            .whereEqualTo("status", "open").get().await()
        for (doc in snap.documents) {
            val found = doc.toObject(FoundItem::class.java) ?: continue
            val score = MatchingEngine.score(lost, found)
            if (score >= 50) saveMatch(lost.id, doc.id, lost.userId, found.userId, score)
        }
    }

    private suspend fun runMatchingForFound(found: FoundItem) {
        val snap = db.collection("lost_items")
            .whereEqualTo("status", "open").get().await()
        for (doc in snap.documents) {
            val lost = doc.toObject(LostItem::class.java) ?: continue
            val score = MatchingEngine.score(lost, found)
            if (score >= 50) saveMatch(doc.id, found.id, lost.userId, found.userId, score)
        }
    }

    private suspend fun saveMatch(
        lostId      : String,
        foundId     : String,
        lostUserId  : String,
        foundUserId : String,
        score       : Int
    ) {
        // The deterministic ID (lostId_foundId) already prevents duplicates
        // The old existence check was removed because it was being silently
        // denied by Firestore security rules on non-existent documents
        val matchId = "${lostId}_${foundId}"
        val match   = Match(
            id          = matchId,
            lostItemId  = lostId,
            foundItemId = foundId,
            lostUserId  = lostUserId,
            foundUserId = foundUserId,
            score       = score,
            status      = "pending",
            createdAt   = System.currentTimeMillis()
        )
        db.collection("matches").document(matchId).set(match).await()
        db.collection("lost_items").document(lostId).update("status", "matched").await()
        db.collection("found_items").document(foundId).update("status", "matched").await()
    }

    suspend fun deleteLostItem(itemId: String): Result<Unit> {
        return try {
            db.collection("lost_items").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteFoundItem(itemId: String): Result<Unit> {
        return try {
            db.collection("found_items").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}