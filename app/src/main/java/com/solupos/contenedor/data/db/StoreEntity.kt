package com.solupos.contenedor.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Al abrir la app entra directo al consultador de esta tienda (equipo fijo en piso). */
    @ColumnInfo(defaultValue = "0") val kioskMode: Boolean = false,
    @ColumnInfo(defaultValue = "0") val kioskFrontCamera: Boolean = false
)
