package com.example.lotteryapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterHistoryDao {
    
    @Query("SELECT * FROM master_history ORDER BY id DESC")
    fun getAll(): Flow<List<MasterHistory>>
    
    @Query("SELECT * FROM master_history")
    suspend fun getAllSync(): List<MasterHistory>
    
    @Query("SELECT * FROM master_history WHERE id = :id")
    suspend fun getById(id: Int): MasterHistory?
    
    @Insert
    suspend fun insert(master: MasterHistory): Long
    
    @Update
    suspend fun update(master: MasterHistory)
    
    @Delete
    suspend fun delete(master: MasterHistory)
    
    @Query("DELETE FROM master_history")
    suspend fun deleteAll()
    
    // Items
    @Query("SELECT * FROM master_items WHERE masterId = :masterId")
    fun getItemsForMaster(masterId: Int): Flow<List<MasterItem>>
    
    @Query("SELECT * FROM master_items WHERE masterId = :masterId")
    suspend fun getItemsForMasterSync(masterId: Int): List<MasterItem>
    
    // Get all master items (for building JSON)
    @Query("SELECT * FROM master_items")
    fun getAllItemsSync(): List<MasterItem>
    
    @Insert
    suspend fun insertItem(item: MasterItem)
    
    @Insert
    suspend fun insertItems(items: List<MasterItem>)
    
    @Query("DELETE FROM master_items WHERE masterId = :masterId")
    suspend fun deleteItemsForMaster(masterId: Int)
    
    @Query("DELETE FROM master_items")
    suspend fun deleteAllItems()
}