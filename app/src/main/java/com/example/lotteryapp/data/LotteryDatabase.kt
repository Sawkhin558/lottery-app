package com.example.lotteryapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Voucher::class, LotteryItem::class],
    version = 1,
    exportSchema = false
)
abstract class LotteryDatabase : RoomDatabase() {
    
    abstract fun voucherDao(): VoucherDao
    abstract fun lotteryItemDao(): LotteryItemDao
    
    companion object {
        @Volatile
        private var INSTANCE: LotteryDatabase? = null
        
        fun getDatabase(context: Context): LotteryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LotteryDatabase::class.java,
                    "lottery_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}