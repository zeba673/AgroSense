package com.example.data

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repositorio de datos que implementa el patrón de repositorio para desacoplar
 * el acceso a datos locales de la interfaz de usuario / vista modelo.
 */
class AgriRepository(private val agriDao: AgriDao) {

    // Contracción reactiva de flujos para actualizar la UI en vivo
    val medicionesClima: Flow<List<MedicionClima>> = agriDao.obtenerMedicionesClima()
    val registrosEconomia: Flow<List<RegistroEconomia>> = agriDao.obtenerRegistrosEconomia()
    val telemetriaDrones: Flow<List<TelemetriaDron>> = agriDao.obtenerTelemetriaDrones()

    // --- Autenticación Local ---

    suspend fun obtenerUsuario(dni: Long): Usuario? {
        return agriDao.obtenerUsuarioPorDni(dni)
    }

    suspend fun registrarNuevoUsuario(dni: Long, nombre: String, contrasegnaLiteral: String): Boolean {
        return try {
            val hash = CifradoUtil.hashSha256(contrasegnaLiteral)
            val nuevo = Usuario(dni = dni, nombre = nombre, contrasenaHash = hash)
            agriDao.registrarUsuario(nuevo) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun verificarDniExistente(dni: Long): Boolean {
        return agriDao.obtenerUsuarioPorDni(dni) != null
    }

    suspend fun actualizarContrasegnaUsuario(dni: Long, nuevaContrasegnaLiteral: String): Boolean {
        return try {
            val nuevoHash = CifradoUtil.hashSha256(nuevaContrasegnaLiteral)
            agriDao.actualizarContrasena(dni, nuevoHash) > 0
        } catch (e: Exception) {
            false
        }
    }

    // --- Agregar Registros de Clima ---

    suspend fun agregarClima(temperatura: Double, humedadSuelo: Double, humedadAire: Double, viento: Double, origen: String): Boolean {
        val nuevaMedicion = MedicionClima(
            fecha = System.currentTimeMillis(),
            temperatura = temperatura,
            humedadSuelo = humedadSuelo,
            humedadAire = humedadAire,
            viento = viento,
            origen = origen
        )
        return agriDao.insertarMedicionClima(nuevaMedicion) > 0
    }

    // --- Agregar Registros Económicos ---

    suspend fun agregarEconomia(tipo: String, concepto: String, rubro: String, monto: Double, cantidad: Double): Boolean {
        val nuevoRegistro = RegistroEconomia(
            fecha = System.currentTimeMillis(),
            tipo = tipo,
            concepto = concepto,
            rubro = rubro,
            monto = monto,
            cantidad = cantidad
        )
        return agriDao.insertarRegistroEconomia(nuevoRegistro) > 0
    }

    // --- Agregar Registro de Drones ---

    suspend fun agregarTelemetria(identificadorDron: String, nivelLiquido: Int, bateria: Int, rutaNombre: String, estadoVuelo: String): Boolean {
        val nuevaTelemetria = TelemetriaDron(
            fecha = System.currentTimeMillis(),
            identificadorDron = identificadorDron,
            nivelLiquido = nivelLiquido,
            bateria = bateria,
            rutaNombre = rutaNombre,
            estadoVuelo = estadoVuelo
        )
        return agriDao.insertarTelemetriaDron(nuevaTelemetria) > 0
    }

    /**
     * Elimina todos los datos simulados o cargados para dar paso a los datos reales de la chacra del usuario.
     */
    suspend fun vaciarDatosDeDemostracion() {
        agriDao.borrarTodoClima()
        agriDao.borrarTodaEconomia()
        agriDao.borrarTodaTelemetria()
    }

    // --- Función para cargar datos iniciales si no existen registros ---
    suspend fun cargarDatosInicialesSiEsNecesario() {
        // Si no hay usuarios en la base, creamos un usuario de demostración
        val totalUsuarios = agriDao.obtenerCantidadUsuarios()
        if (totalUsuarios == 0) {
            // Usuario demostrativo: DNI 123456, Contraseña: admin
            registrarNuevoUsuario(123456L, "Productor Agrícola", "admin")
        }

        // Cargar datos climáticos iniciales piloto si están vacíos
        // (usamos un truco con flujos o carga secuencial, verificando registros)
        // Insertamos datos iniciales de economía y drones si la base está nueva
        val medicionesPiloto = listOf(
            MedicionClima(fecha = System.currentTimeMillis() - 72000000, temperatura = 21.5, humedadSuelo = 42.0, humedadAire = 55.0, viento = 12.0, origen = "Sensor Lote Norte"),
            MedicionClima(fecha = System.currentTimeMillis() - 36000000, temperatura = 24.0, humedadSuelo = 40.5, humedadAire = 50.0, viento = 15.0, origen = "Sensor Lote Norte"),
            MedicionClima(fecha = System.currentTimeMillis(), temperatura = 18.5, humedadSuelo = 38.0, humedadAire = 60.5, viento = 8.5, origen = "Ingreso Manual")
        )
        
        val economiaPiloto = listOf(
            RegistroEconomia(fecha = System.currentTimeMillis() - 864000000, tipo = "Egreso", concepto = "Sufijo Fertilizante Orgánico", rubro = "Insumos", monto = 120000.0, cantidad = 5.0),
            RegistroEconomia(fecha = System.currentTimeMillis() - 604800000, tipo = "Ingreso", concepto = "Venta Ganado Bovino", rubro = "Ganado", monto = 950000.0, cantidad = 4.0),
            RegistroEconomia(fecha = System.currentTimeMillis() - 345600000, tipo = "Egreso", concepto = "Repuestos de Tractor Jhon", rubro = "Maquinaria", monto = 45000.0, cantidad = 1.0),
            RegistroEconomia(fecha = System.currentTimeMillis() - 86400000, tipo = "Ingreso", concepto = "Carga Venta de Manzana Gala", rubro = "Fruta", monto = 480000.0, cantidad = 1200.0)
        )

        val dronesPiloto = listOf(
            TelemetriaDron(fecha = System.currentTimeMillis() - 172800000, identificadorDron = "Dron Pulverizador X-1", nivelLiquido = 10, bateria = 12, rutaNombre = "Ruta Perímetro Sur", estadoVuelo = "Exitoso"),
            TelemetriaDron(fecha = System.currentTimeMillis() - 86400000, identificadorDron = "Dron Supervisor S-4", nivelLiquido = 0, bateria = 45, rutaNombre = "Mapa Relieve Lote Central", estadoVuelo = "Exitoso")
        )

        // Usamos suspend calls del DAO para asegurar que se insertan en primer login
        // Se autocompleta para que el tablero inicie de forma asombrosa
        try {
            // Evaluamos si ya hay registros antes de insertar para evitar duplicar
            if (agriDao.obtenerCantidadClima() == 0) {
                medicionesPiloto.forEach { agriDao.insertarMedicionClima(it) }
            }
            if (agriDao.obtenerCantidadEconomia() == 0) {
                economiaPiloto.forEach { agriDao.insertarRegistroEconomia(it) }
            }
            if (agriDao.obtenerCantidadTelemetria() == 0) {
                dronesPiloto.forEach { agriDao.insertarTelemetriaDron(it) }
            }
        } catch (_: Exception) {
            // Ya existen o falló la precarga silenciosamente
        }
    }
}
