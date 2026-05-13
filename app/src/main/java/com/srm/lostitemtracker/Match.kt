package com.srm.lostitemtracker

data class Match(
    val id          : String = "",
    val lostItemId  : String = "",
    val foundItemId : String = "",
    val lostUserId  : String = "",
    val foundUserId : String = "",
    val score       : Int    = 0,
    val status      : String = "pending",
    val createdAt   : Long   = 0L
)