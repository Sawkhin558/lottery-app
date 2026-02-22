package com.example.lotteryapp.data

import androidx.room.*

@Dao
interface VoucherDao {
    
    @Query("SELECT * FROM vouchers ORDER BY timestamp DESC")
    suspend fun getAllVouchers(): List<Voucher>
    
    // Synchronous version for bridge (no Flow)
    @Query("SELECT * FROM vouchers ORDER BY timestamp DESC")
    fun getAllVouchersSync(): List<Voucher>
    
    @Insert
    suspend fun insertVoucher(voucher: Voucher): Long
    
    // Get all items (for building combined JSON)
    @Query("SELECT * FROM lottery_items")
    fun getAllItemsSync(): List<LotteryItem>
    
    @Query("SELECT SUM(totalAmount) FROM vouchers")
    suspend fun getTotalAmount(): Int?
    
    @Query("SELECT COUNT(*) FROM vouchers")
    suspend fun getVoucherCount(): Int
    
    @Query("DELETE FROM vouchers")
    suspend fun deleteAllVouchers()
}