package com.example.lotteryapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vouchers")
data class Voucher(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val rawText: String,
    val totalAmount: Int,
    val timestamp: Long = System.currentTimeMillis()
)