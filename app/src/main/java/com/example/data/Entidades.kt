package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Usuario para autenticación 100% local en el dispositivo.
 * El DNI sirve como identificador único y formato numérico para el inicio de sesión.
 */
@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dni: Long, // Documento Nacional de Identidad
    val nombre: String,
    val contrasenaHash: String // Almacenamiento encriptado de la contraseña
)

/**
 * Entidad de Clima para registrar las mediciones meteorológicas de la chacra.
 */
@Entity(tableName = "mediciones_clima")
data class MedicionClima(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fecha: Long, // Marca de tiempo (timestamp)
    val temperatura: Double, // en grados Celsius
    val humedadSuelo: Double, // porcentaje 0-100
    val humedadAire: Double, // porcentaje 0-100
    val viento: Double, // velocidad en km/h
    val origen: String // "Sensor Automático" o "Ingreso Manual"
)

/**
 * Entidad de Economía para registrar gastos e ingresos de insumos, ganado o fruta.
 */
@Entity(tableName = "registros_economia")
data class RegistroEconomia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fecha: Long, // Marca de tiempo (timestamp)
    val tipo: String, // "Ingreso" o "Egreso"
    val concepto: String, // ej. "Venta de Manzanas", "Alimento Ganado"
    val rubro: String, // "Insumos", "Ganado", "Fruta", "Maquinaria", "Otros"
    val monto: Double, // Valor en pesos
    val cantidad: Double // Cantidad asociada (ej. 250.0 kg o 15 vacas)
)

/**
 * Entidad de Telemetría de Drones para guardar los estados de las rutas recopiladas localmente.
 */
@Entity(tableName = "telemetria_drones")
data class TelemetriaDron(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fecha: Long, // Marca de tiempo (timestamp)
    val identificadorDron: String, // ej. "Dron Alfa-1"
    val nivelLiquido: Int, // porcentaje de líquido restante 0-100
    val bateria: Int, // porcentaje de batería restante 0-100
    val rutaNombre: String, // Nombre del trayecto volado
    val estadoVuelo: String // "Exitoso", "Interrumpido", "Batería Crítica"
)
