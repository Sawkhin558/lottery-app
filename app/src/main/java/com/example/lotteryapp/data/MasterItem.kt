package com.example.lotteryapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "master_items",
    foreignKeys = [
        ForeignKey(
            entity = MasterHistory::class,
            parentColumns = ["id"],
            childColumns = ["masterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("masterId")]
)
data class MasterItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val masterId: Int,
    val number: String,
    val directAmount: Int,
    val rolledAmount: Int
)