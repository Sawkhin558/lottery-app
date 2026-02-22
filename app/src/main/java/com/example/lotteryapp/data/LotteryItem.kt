package com.example.lotteryapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "lottery_items",
    foreignKeys = [ForeignKey(
        entity = Voucher::class,
        parentColumns = ["id"],
        childColumns = ["voucherId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class LotteryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    var voucherId: Int = 0,
    val number: String,
    val directAmount: Int,
    val rolledAmount: Int,
    val type: String
) {
    val totalAmount: Int
        get() = directAmount + rolledAmount
}