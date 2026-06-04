package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos SQLite local para la aplicación de Gestión Agrícola.
 * Administra de manera persistente y autónoma toda la información de la chacra en el procesador local.
 */
@Database(
    entities = [
        Usuario::class,
        MedicionClima::class,
        RegistroEconomia::class,
        TelemetriaDron::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AgriDatabase : RoomDatabase() {

    abstract fun agriDao(): AgriDao

    companion object {
        @Volatile
        private var INSTANCE: AgriDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos (Patrón Singleton).
         */
        fun obtenerInstancia(context: Context): AgriDatabase {
            return INSTANCE ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    AgriDatabase::class.java,
                    "chacra_agricola_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instancia
                instancia
            }
        }
    }
}
