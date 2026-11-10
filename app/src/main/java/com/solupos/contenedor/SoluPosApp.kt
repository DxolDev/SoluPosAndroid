package com.solupos.contenedor

import android.app.Application
import androidx.room.Room
import com.solupos.contenedor.data.db.AppDatabase
import com.solupos.contenedor.data.preferences.UserPreferences
import com.solupos.contenedor.data.repository.StoreRepository
import com.solupos.contenedor.pricecheck.PriceLookupProvider
import com.solupos.contenedor.pricecheck.PriceLookupService
import com.solupos.contenedor.printer.BluetoothPrinterManager
import com.solupos.contenedor.security.KeystoreTokenStore
import com.solupos.contenedor.security.KioskPinStore
import com.solupos.contenedor.security.TokenStore

class SoluPosApp : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "solupos.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
    val storeRepository: StoreRepository by lazy {
        StoreRepository(database.storeDao(), userPreferences)
    }
    val printerManager: BluetoothPrinterManager by lazy { BluetoothPrinterManager() }

    val tokenStore: TokenStore by lazy { KeystoreTokenStore(this) }
    val kioskPinStore: KioskPinStore by lazy { KioskPinStore(this) }

    /** Se reemplaza por el cliente del proxy cuando esté definido dónde se aloja. */
    val priceLookup: PriceLookupService by lazy { PriceLookupProvider.create() }
}
