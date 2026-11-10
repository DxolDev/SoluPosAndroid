package com.solupos.contenedor.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [StoreEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    companion object {
        /** Consultador de precios: modo quiosco y cámara frontal por tienda. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stores ADD COLUMN kioskMode INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stores ADD COLUMN kioskFrontCamera INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
