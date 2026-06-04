package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz de Acceso a Datos (DAO) para interactuar con la base de datos SQLite local.
 * Toda la gestión ocurre de manera autónoma en el dispositivo Android.
 */
@Dao
interface AgriDao {

    // --- Módulo de Usuarios y Autenticación ---

    @Query("SELECT * FROM usuarios WHERE dni = :dni LIMIT 1")
    suspend fun obtenerUsuarioPorDni(dni: Long): Usuario?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registrarUsuario(usuario: Usuario): Long

    @Query("UPDATE usuarios SET contrasenaHash = :nuevoHash WHERE dni = :dni")
    suspend fun actualizarContrasena(dni: Long, nuevoHash: String): Int

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun obtenerCantidadUsuarios(): Int

    @Query("SELECT COUNT(*) FROM mediciones_clima")
    suspend fun obtenerCantidadClima(): Int

    @Query("SELECT COUNT(*) FROM registros_economia")
    suspend fun obtenerCantidadEconomia(): Int

    @Query("SELECT COUNT(*) FROM telemetria_drones")
    suspend fun obtenerCantidadTelemetria(): Int

    // --- Módulo Meteorológico / Clima ---

    @Query("SELECT * FROM mediciones_clima ORDER BY fecha DESC")
    fun obtenerMedicionesClima(): Flow<List<MedicionClima>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarMedicionClima(medicion: MedicionClima): Long

    @Query("DELETE FROM mediciones_clima")
    suspend fun borrarTodoClima()

    // --- Módulo de Economía y Finanzas ---

    @Query("SELECT * FROM registros_economia ORDER BY fecha DESC")
    fun obtenerRegistrosEconomia(): Flow<List<RegistroEconomia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarRegistroEconomia(registro: RegistroEconomia): Long

    @Query("DELETE FROM registros_economia")
    suspend fun borrarTodaEconomia()

    // --- Módulo de Drones Agrícolas ---

    @Query("SELECT * FROM telemetria_drones ORDER BY fecha DESC")
    fun obtenerTelemetriaDrones(): Flow<List<TelemetriaDron>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTelemetriaDron(telemetria: TelemetriaDron): Long

    @Query("DELETE FROM telemetria_drones")
    suspend fun borrarTodaTelemetria()
}
