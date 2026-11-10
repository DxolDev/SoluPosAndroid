package com.solupos.contenedor.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores ORDER BY createdAt ASC")
    fun getAll(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE id = :id")
    suspend fun getById(id: String): StoreEntity?

    @Query("SELECT * FROM stores WHERE kioskMode = 1 ORDER BY createdAt ASC LIMIT 1")
    suspend fun getKioskStore(): StoreEntity?

    @Query("UPDATE stores SET kioskMode = 0 WHERE id != :id")
    suspend fun clearKioskExcept(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(store: StoreEntity)

    @Update
    suspend fun update(store: StoreEntity)

    @Delete
    suspend fun delete(store: StoreEntity)
}
