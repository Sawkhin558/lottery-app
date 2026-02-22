package com.example.lotteryapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_history")
data class MasterHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val time: String,
    val total: Int
)