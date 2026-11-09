package com.boletodigital.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Boleto::class], version = 1, exportSchema = false)
abstract class BoletoDatabase : RoomDatabase() {
    abstract fun boletoDao(): BoletoDao

    companion object {
        @Volatile
        private var INSTANCE: BoletoDatabase? = null

        fun get(context: Context): BoletoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BoletoDatabase::class.java,
                    "boleto_digital.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
