package com.example.lotteryapp.data

import androidx.room.*

@Dao
interface LotteryItemDao {
    
    @Insert
    suspend fun insertItem(item: LotteryItem)
    
    @Query("SELECT * FROM lottery_items WHERE voucherId = :voucherId")
    suspend fun getItemsByVoucher(voucherId: Int): List<LotteryItem>
    
    @Query("SELECT SUM(directAmount + rolledAmount) FROM lottery_items")
    suspend fun getTotalAmount(): Int?
    
    @Query("DELETE FROM lottery_items")
    suspend fun deleteAllItems()
    
    // Sync version for bridge
    @Query("SELECT * FROM lottery_items")
    fun getAllItemsSync(): List<LotteryItem>
}