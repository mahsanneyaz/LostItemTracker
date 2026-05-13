package com.srm.lostitemtracker

import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

object MatchingEngine {

    // Total score is out of 100:
    // Category:  30 points
    // Name:      25 points
    // Location:  15 points
    // Date:      10 points
    // Image:     20 points (only applies when both items have photos)

    fun score(lost: LostItem, found: FoundItem): Int {
        var total = 0

        // 1 — Category match (30 points)
        if (lost.category.equals(found.category, ignoreCase = true)) {
            total += 30
        }

        // 2 — Item name similarity (up to 25 points)
        total += nameScore(lost.itemName, found.itemName)

        // 3 — Location similarity (up to 15 points)
        total += locationScore(lost.location, found.location)

        // 4 — Date proximity (up to 10 points)
        total += dateScore(lost.date, found.date)

        // 5 — Image label similarity (up to 20 points)
        // This only adds points when both items have photos with detected labels
        // It never penalises items that have no photo (returns 0 if either list is empty)
        total += imageScore(lost.imageLabels, found.imageLabels)

        return total.coerceIn(0, 100)
    }

    // Splits both names into words and measures overlap (Jaccard similarity)
    private fun nameScore(lostName: String, foundName: String): Int {
        val lostTokens  = tokenise(lostName)
        val foundTokens = tokenise(foundName)
        if (lostTokens.isEmpty() || foundTokens.isEmpty()) return 0

        val intersection = lostTokens.intersect(foundTokens).size
        val union        = lostTokens.union(foundTokens).size
        val similarity   = intersection.toDouble() / union.toDouble()
        return (similarity * 25).toInt()
    }

    // Checks if location strings share keywords
    private fun locationScore(lostLoc: String, foundLoc: String): Int {
        val lostTokens  = tokenise(lostLoc)
        val foundTokens = tokenise(foundLoc)
        if (lostTokens.isEmpty() || foundTokens.isEmpty()) return 0

        val intersection = lostTokens.intersect(foundTokens).size
        val union        = lostTokens.union(foundTokens).size
        val similarity   = intersection.toDouble() / union.toDouble()
        return (similarity * 15).toInt()
    }

    // Same day: 10 points. Within 1 day: 7. Within 3 days: 4.
    private fun dateScore(lostDate: String, foundDate: String): Int {
        return try {
            val fmt  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d1   = fmt.parse(lostDate)  ?: return 0
            val d2   = fmt.parse(foundDate) ?: return 0
            val diff = abs(d1.time - d2.time) / (1000 * 60 * 60 * 24)
            when {
                diff == 0L -> 10
                diff <= 1L ->  7
                diff <= 3L ->  4
                else       ->  0
            }
        } catch (e: Exception) { 0 }
    }

    // Compares ML Kit labels from both photos using Jaccard similarity
    // Returns 0 if either item has no labels (no photo, or labeling failed)
    // This means the image score never hurts items that have no photo
    private fun imageScore(lostLabels: List<String>, foundLabels: List<String>): Int {
        if (lostLabels.isEmpty() || foundLabels.isEmpty()) return 0

        val lostSet  = lostLabels.map  { it.lowercase() }.toSet()
        val foundSet = foundLabels.map { it.lowercase() }.toSet()

        val intersection = lostSet.intersect(foundSet).size
        val union        = lostSet.union(foundSet).size
        if (union == 0) return 0

        val similarity = intersection.toDouble() / union.toDouble()
        return (similarity * 20).toInt()
    }

    // Splits a string into lowercase words, removing short filler words
    private fun tokenise(text: String): Set<String> {
        return text.lowercase()
            .split(" ", ",", "-", "/", ".", "_")
            .map { it.trim() }
            .filter { it.length > 1 }
            .toSet()
    }
}