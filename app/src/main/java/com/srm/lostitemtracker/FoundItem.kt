package com.srm.lostitemtracker

data class FoundItem(
    val id          : String       = "",
    val userId      : String       = "",
    val itemName    : String       = "",
    val category    : String       = "",
    val description : String       = "",
    val location    : String       = "",
    val date        : String       = "",
    val photoUrl    : String       = "",
    val status      : String       = "open",
    val createdAt   : Long         = 0L,
    val imageLabels : List<String> = emptyList()   // ML Kit detected labels from the photo
)