package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.AsistenteIA
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * ViewModel central para administrar el flujo de datos de la chacra y la autenticación local.
 * Sigue los patrones de ViewModel y arquitectura limpia definidos en las guías técnicas.
 */
class AgriViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AgriRepository

    // Flujos de datos reactivos obtenidos directamente de SQLite (Room)
    val climaHistorial: StateFlow<List<MedicionClima>>
    val finanzasHistorial: StateFlow<List<RegistroEconomia>>
    val dronesHistorial: StateFlow<List<TelemetriaDron>>

    // --- Estado de Autenticación y Navegación ---
    var usuarioLogueado by mutableStateOf<Usuario?>(null)
        private set

    var pestagnaSeleccionada by mutableStateOf("tablero") // tablero, clima, drones, economia, topografia

    var mensajeErrorAuth by mutableStateOf("")
    var mensajeExitoAuth by mutableStateOf("")
    var cargandoAuth by mutableStateOf(false)

    // --- Estado de la IA ---
    var respuestaIA by mutableStateOf("")
        private set
    var cargandoIA by mutableStateOf(false)
        private set
    var consultaUsuarioIA by mutableStateOf("")

    // --- Entradas del Formulario de Economía y Proyecciones Locales ---
    // Valores por defecto representativos de una chacra patagónica o pampeana
    var costoPorHectarea by mutableStateOf("45000")
    var hectareasCultivo by mutableStateOf("30")
    var precioVentaEstimado by mutableStateOf("120") // pesos por kg de fruta
    var rendimientoKilosPorHectarea by mutableStateOf("8000") // kg por hectárea
    
    var precioCompraGanado by mutableStateOf("150000") // pesos por cabeza
    var cantidadCabezas by mutableStateOf("40")
    var precioVentaGanado by mutableStateOf("280000") // pesos por cabeza peso final

    // --- Estado de la Topografía Interactiva ---
    var inclinacionX by mutableStateOf(45f) // ángulo de rotación de red 3D
    var inclinacionY by mutableStateOf(30f)
    var multiplicadorElevacion by mutableStateOf(1.0f)

    // --- ESTADO DE REAL-TIME WEATHER & GPS UBICACIÓN ---
    var latitudCelular by mutableStateOf(-39.030) // Fallback default (Patagonia Fruit region / General Roca coordinates)
    var longitudCelular by mutableStateOf(-67.580) // Fallback default
    var ubicacionObtenidaPorGps by mutableStateOf(false)
    var cargandoUbicacionGps by mutableStateOf(false)
    var mensajeEstadoClimaReal by mutableStateOf("Coordenadas: General Roca, Patagonia Argentina")

    // Variables de clima en tiempo real desde API libre (Open-Meteo)
    var tempRealTime by mutableStateOf<Double?>(null)
    var humAireRealTime by mutableStateOf<Double?>(null)
    var vientoRealTime by mutableStateOf<Double?>(null)
    var humSueloRealTime by mutableStateOf<Double?>(null)
    var climaCargandoRealTime by mutableStateOf(false)
    var climaErrorRealTime by mutableStateOf<String?>(null)

    // --- ESTADO DE COTIZACION EN TIEMPO REAL (ARGENTINA & INTERNACIONAL) ---
    var cotizacionDolarBlue by mutableStateOf<Double?>(null)
    var cotizacionDolarOficial by mutableStateOf<Double?>(null)
    var cargandoFinanzasApi by mutableStateOf(false)
    var errorFinanzasApi by mutableStateOf<String?>(null)

    // Índices calculados en pesos (ARS) ajustados dinámicamente con Dólar Blue
    var precioTrigoArs by mutableStateOf(268400.0) // Trigo Rosario FOB ref (USD 220 * Blue)
    var precioSojaArs by mutableStateOf(506300.0)  // Soja Argentina ref (USD 415 * Blue)
    var precioCarneArs by mutableStateOf(2257.0)    // Carne Vacuna Liniers Kg (USD 1.85 * Blue)
    var precioManzanaArs by mutableStateOf(1037.0)  // Manzana fresca Kg (USD 0.85 * Blue)

    // --- SIMULACIÓN AVANZADA DE DRONES DJI AGRAS T100 ---
    var dronesActivos by mutableStateOf(listOf(
        DronActivo("DJI-Agras-T100-01", "DJI Agras T100 (Modular)", 88, 70, 100, 15, 12, "Fumigando Lote Manzanas Sector A", "FUMIGANDO", -39.0315, -67.5822),
        DronActivo("DJI-Agras-T100-02", "DJI Agras T100 (Modular)", 24, 6, 100, 22, 19, "Bajo nivel de químico - Volviendo a base", "RETORNANDO", -39.0298, -67.5795),
        DronActivo("DJI-Mavic-Ent-03", "DJI Mavic 3 Multispectral", 92, 0, 0, 18, 8, "Escaneo Multiespectral y Relieve Lote B", "VIGILANDO", -39.0285, -67.5840)
    ))

    // --- RELIEVE GPS SECTOR SELECCIONADO ---
    var deFilaUsuario by mutableStateOf(3) // Fila en mallas 3D
    var deColUsuario by mutableStateOf(3) // Columna en mallas 3D
    var altitudFilaCelular by mutableStateOf(45.0)

    // Matriz de relieve local cargada (representa alturas en metros)
    var matrizRelieve by mutableStateOf(
        arrayOf(
            floatArrayOf(10f, 15f, 20f, 18f, 12f, 8f, 5f),
            floatArrayOf(12f, 18f, 25f, 30f, 22f, 15f, 10f),
            floatArrayOf(15f, 22f, 35f, 40f, 32f, 20f, 12f),
            floatArrayOf(14f, 25f, 38f, 45f, 35f, 22f, 11f),
            floatArrayOf(10f, 18f, 30f, 32f, 25f, 18f, 8f),
            floatArrayOf(8f, 12f, 15f, 18f, 15f, 10f, 5f),
            floatArrayOf(5f, 6f, 8f, 10f, 8f, 5f, 3f)
        )
    )

    init {
        val database = AgriDatabase.obtenerInstancia(application)
        repository = AgriRepository(database.agriDao())

        climaHistorial = repository.medicionesClima.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        finanzasHistorial = repository.registrosEconomia.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        dronesHistorial = repository.telemetriaDrones.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Se inicializan y precargan datos de demostración si es el primer inicio de la app
        viewModelScope.launch {
            repository.cargarDatosInicialesSiEsNecesario()
        }

        // Cargar cotizaciones iniciales en segundo plano de inmediato de la API nacional
        viewModelScope.launch {
            consultarCotizacionesEnTiempoReal()
        }

        // Iniciar hilo de simulación activa de drones agrícolas (DJI Agras T100 style)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(4500) // actualizar telemetría cada 4.5 segundos
                val actualizados = dronesActivos.map { dron ->
                    var bat = dron.bateria
                    var liq = dron.nivelLiquido
                    var estado = dron.estado
                    var act = dron.actividad
                    var tv = dron.tiempoVueloMinutos
                    var vel = dron.velocidad
                    var lat = dron.latitud
                    var lng = dron.longitud

                    when (estado) {
                        "FUMIGANDO" -> {
                            bat -= 1
                            liq -= 1
                            tv += 1
                            lat += (Math.random() - 0.5) * 0.0002
                            lng += (Math.random() - 0.5) * 0.0002
                            if (liq <= 5) {
                                estado = "RETORNANDO"
                                act = "Depósito casi vacío. Retornando automáticamente para recarga."
                                vel = 24
                            } else if (bat <= 20) {
                                estado = "RETORNANDO"
                                act = "Batería baja. Iniciando protocolo de retorno a base."
                                vel = 24
                            }
                        }
                        "RETORNANDO" -> {
                            bat -= 1
                            tv += 1
                            val baseLat = -39.030
                            val baseLng = -67.580
                            val diffLat = baseLat - lat
                            val diffLng = baseLng - lng
                            lat += diffLat * 0.4
                            lng += diffLng * 0.4
                            if (Math.abs(diffLat) < 0.0005 && Math.abs(diffLng) < 0.0005) {
                                estado = "CARGANDO EN BASE"
                                act = "Aterrizado. Abasteciendo líquido y cargando celdas de energía."
                                vel = 0
                            }
                        }
                        "VIGILANDO" -> {
                            bat -= 1
                            tv += 1
                            lat += (Math.random() - 0.5) * 0.0003
                            lng += (Math.random() - 0.5) * 0.0003
                            if (bat <= 20) {
                                estado = "RETORNANDO"
                                act = "Batería baja del sensor de barrido. Retornando a base."
                                vel = 22
                            }
                        }
                        "CARGANDO EN BASE" -> {
                            bat += 12
                            liq += 15
                            if (bat >= 100) bat = 100
                            if (liq >= dron.capacidadMaxima) liq = dron.capacidadMaxima
                            if (bat == 100 && (dron.capacidadMaxima == 0 || liq == dron.capacidadMaxima)) {
                                if (dron.capacidadMaxima == 0) {
                                    estado = "VIGILANDO"
                                    act = "Vuelo de escaneo topográfico y seguridad perimetral."
                                    tv = 0
                                    vel = 18
                                } else {
                                    estado = "FUMIGANDO"
                                    act = "Fumigación de parcelas frutales Sector A con Agras T100."
                                    tv = 0
                                    vel = 15
                                }
                            }
                        }
                    }

                    if (bat < 0) bat = 0
                    if (liq < 0) liq = 0

                    dron.copy(
                        bateria = bat,
                        nivelLiquido = liq,
                        estado = estado,
                        actividad = act,
                        tiempoVueloMinutos = tv,
                        velocidad = vel,
                        latitud = lat,
                        longitud = lng
                    )
                }
                dronesActivos = actualizados
            }
        }
    }

    // --- Operaciones de Autenticación Local ---
 
    /**
     * Intenta iniciar sesión comparando el DNI y la contraseña de manera local.
     * Aplica sanitización trim() para evitar espacios indeseados al final (típicos de teclados móviles).
     */
    fun iniciarSesion(dniTexto: String, contrasegna: String) {
        mensajeErrorAuth = ""
        mensajeExitoAuth = ""
        val dniLimpio = dniTexto.trim()
        val dni = dniLimpio.toLongOrNull()
        if (dni == null) {
            mensajeErrorAuth = "El DNI ingresado debe ser un número válido."
            return
        }
        val contrasegnaLimpia = contrasegna.trim()
        if (contrasegnaLimpia.isEmpty()) {
            mensajeErrorAuth = "Por favor ingrese su contraseña."
            return
        }

        cargandoAuth = true
        viewModelScope.launch {
            try {
                val usuarioEncontrado = repository.obtenerUsuario(dni)
                if (usuarioEncontrado != null) {
                    val codificada = CifradoUtil.hashSha256(contrasegnaLimpia)
                    if (usuarioEncontrado.contrasenaHash == codificada) {
                        usuarioLogueado = usuarioEncontrado
                        mensajeExitoAuth = "¡Bienvenido de vuelta, ${usuarioEncontrado.nombre}!"
                    } else {
                        mensajeErrorAuth = "La contraseña ingresada es incorrecta."
                    }
                } else {
                    mensajeErrorAuth = "No se encontró ningún usuario registrado con el DNI $dni."
                }
            } catch (e: Exception) {
                mensajeErrorAuth = "Error en base de datos local: ${e.localizedMessage}"
            } finally {
                cargandoAuth = false
            }
        }
    }

    /**
     * Registra un nuevo usuario de manera local y lo auto-loguea.
     * Aplica sanitización trim() para evitar espacios indeseados al final.
     */
    fun registrarUsuario(dniTexto: String, nombre: String, contrasegna: String) {
        mensajeErrorFormularios()
        val dniLimpio = dniTexto.trim()
        val dni = dniLimpio.toLongOrNull()
        if (dni == null) {
            mensajeErrorAuth = "El DNI debe contener solo números."
            return
        }
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty()) {
            mensajeErrorAuth = "El nombre no puede estar vacío."
            return
        }
        val contrasegnaLimpia = contrasegna.trim()
        if (contrasegnaLimpia.length < 4) {
            mensajeErrorAuth = "La contraseña debe tener al menos 4 caracteres."
            return
        }

        cargandoAuth = true
        viewModelScope.launch {
            try {
                if (repository.verificarDniExistente(dni)) {
                    mensajeErrorAuth = "El DNI $dni ya se encuentra registrado localmente."
                } else {
                    val exito = repository.registrarNuevoUsuario(dni, nombreLimpio, contrasegnaLimpia)
                    if (exito) {
                        val nuevoUsuario = repository.obtenerUsuario(dni)
                        usuarioLogueado = nuevoUsuario
                        mensajeExitoAuth = "Registro exitoso en el almacenamiento seguro local."
                    } else {
                        mensajeErrorAuth = "Surgió un error al escribir o encriptar el usuario."
                    }
                }
            } catch (e: Exception) {
                mensajeErrorAuth = "Error de registro: ${e.localizedMessage}"
            } finally {
                cargandoAuth = false
            }
        }
    }

    /**
     * Reestablece la contraseña de manera local autónoma para el usuario del campo.
     */
    fun reestablecerContrasegnaLocal(dniTexto: String, nuevaContrasegna: String) {
        mensajeErrorFormularios()
        val dniLimpio = dniTexto.trim()
        val dni = dniLimpio.toLongOrNull()
        if (dni == null) {
            mensajeErrorAuth = "El DNI debe contener solo números."
            return
        }
        val contrasegnaLimpia = nuevaContrasegna.trim()
        if (contrasegnaLimpia.length < 4) {
            mensajeErrorAuth = "La nueva contraseña debe tener al menos 4 caracteres."
            return
        }

        cargandoAuth = true
        viewModelScope.launch {
            try {
                val existe = repository.verificarDniExistente(dni)
                if (existe) {
                    val exito = repository.actualizarContrasegnaUsuario(dni, contrasegnaLimpia)
                    if (exito) {
                        mensajeExitoAuth = "¡Contraseña reestablecida! Ya puede iniciar sesión con su nueva clave local."
                    } else {
                        mensajeErrorAuth = "No se pudo actualizar la contraseña local."
                    }
                } else {
                    mensajeErrorAuth = "No existe ningún usuario registrado con el DNI $dni."
                }
            } catch (e: Exception) {
                mensajeErrorAuth = "Error al actualizar clave: ${e.localizedMessage}"
            } finally {
                cargandoAuth = false
            }
        }
    }

    /**
     * Limpia los mensajes y errores de autenticación.
     */
    fun limpiarMensajesAuth() {
        mensajeErrorAuth = ""
        mensajeExitoAuth = ""
    }

    /**
     * Vacía todas las mediciones climatológicas, balances económicos y telemetrías cargadas.
     */
    fun vaciarTablasYDatosDemostrativos() {
        viewModelScope.launch {
            repository.vaciarDatosDeDemostracion()
        }
    }

    /**
     * Importa registros reales parseando un string JSON.
     */
    fun importarDatosEstiloJson(jsonString: String): String {
        return try {
            val root = org.json.JSONObject(jsonString)
            
            var climaImportados = 0
            var economiaImportados = 0
            var telemetriaImportados = 0

            viewModelScope.launch {
                // Importar Clima
                val climaArray = root.optJSONArray("clima")
                if (climaArray != null) {
                    for (i in 0 until climaArray.length()) {
                        val obj = climaArray.getJSONObject(i)
                        repository.agregarClima(
                            temperatura = obj.optDouble("temperatura", 20.0),
                            humedadSuelo = obj.optDouble("humedadSuelo", 40.0),
                            humedadAire = obj.optDouble("humedadAire", 50.0),
                            viento = obj.optDouble("viento", 10.0),
                            origen = obj.optString("origen", "Importado Real")
                        )
                        climaImportados++
                    }
                }

                // Importar Economía
                val economiaArray = root.optJSONArray("economia")
                if (economiaArray != null) {
                    for (i in 0 until economiaArray.length()) {
                        val obj = economiaArray.getJSONObject(i)
                        repository.agregarEconomia(
                            tipo = obj.optString("tipo", "Egreso"),
                            concepto = obj.optString("concepto", "Insumo Real"),
                            rubro = obj.optString("rubro", "Insumos"),
                            monto = obj.optDouble("monto", 0.0),
                            cantidad = obj.optDouble("cantidad", 1.0)
                        )
                        economiaImportados++
                    }
                }

                // Importar Telemetría
                val telemetriaArray = root.optJSONArray("telemetria")
                if (telemetriaArray != null) {
                    for (i in 0 until telemetriaArray.length()) {
                        val obj = telemetriaArray.getJSONObject(i)
                        repository.agregarTelemetria(
                            identificadorDron = obj.optString("identificadorDron", "Dron DJI Real"),
                            nivelLiquido = obj.optInt("nivelLiquido", 100),
                            bateria = obj.optInt("bateria", 100),
                            rutaNombre = obj.optString("rutaNombre", "Ruta Real"),
                            estadoVuelo = obj.optString("estadoVuelo", "Exitoso")
                        )
                        telemetriaImportados++
                    }
                }
            }

            "Se importaron con éxito: $climaImportados climas, $economiaImportados economías y $telemetriaImportados telemetrías."
        } catch (e: Exception) {
            "Error al analizar el JSON: ${e.localizedMessage}"
        }
    }

    fun cerrarSesion() {
        usuarioLogueado = null
        respuestaIA = ""
        pestagnaSeleccionada = "tablero"
        mensajeErrorAuth = ""
        mensajeExitoAuth = ""
    }

    private fun mensajeErrorFormularios() {
        mensajeErrorAuth = ""
        mensajeExitoAuth = ""
    }

    // --- Operaciones Climáticas ---

    fun registrarMedicionClimaManual(temp: Double, humSuelo: Double, humAire: Double, vientoVel: Double) {
        viewModelScope.launch {
            repository.agregarClima(
                temperatura = temp,
                humedadSuelo = humSuelo,
                humedadAire = humAire,
                viento = vientoVel,
                origen = "Ingreso Manual"
            )
        }
    }

    // --- Operaciones Económicas ---

    fun registrarGastoIngresoForm(tipo: String, concepto: String, rubro: String, monto: Double, cantidad: Double) {
        viewModelScope.launch {
            repository.agregarEconomia(
                tipo = tipo,
                concepto = concepto,
                rubro = rubro,
                monto = monto,
                cantidad = cantidad
            )
        }
    }

    // --- Operaciones de Drones ---

    fun simularImportacionDeDronCable() {
        viewModelScope.launch {
            // Simulamos datos de un dron agrícola leídos por cable de datos local físico
            val nombresRutas = listOf("Lote Manzana 3", "Cortina Viento Este", "Perímetro Bovino", "Lote Pera Norte")
            val nivelLiq = (15..95).random()
            val bat = (25..99).random()
            val estado = if (bat < 30) "Batería Crítica" else "Exitoso"

            repository.agregarTelemetria(
                identificadorDron = "Dron Pulverizador X-" + (1..9).random(),
                nivelLiquido = nivelLiq,
                bateria = bat,
                rutaNombre = nombresRutas.random(),
                estadoVuelo = estado
            )
        }
    }

    // --- Importar Topografía de archivo local ---

    fun simularCargaArchivoTopograficoDEM() {
        // Simular la carga de un archivo topográfico modelo .DEM (Digital Elevation Model)
        // Alteramos la matriz de relieve agregando diferentes lomas y declives locales de chacra
        val nuevaMatriz = Array(7) { FloatArray(7) }
        for (i in 0..6) {
            for (j in 0..6) {
                // Generamos declive con lomas aleatorias orgánicas
                nuevaMatriz[i][j] = (5..15).random().toFloat() + (i * j) + (if (i == 3 && j == 3) 25f else 0f)
            }
        }
        matrizRelieve = nuevaMatriz
    }

    // --- Consulta al Asesor de IA (Única Conectividad Externa de la Aplicación) ---

    fun consultarAsesorIA() {
        if (cargandoIA) return
        cargandoIA = true
        respuestaIA = "Analizando base de datos local y conectando con el modelo Gemini Flash vía OpenRouter..."

        viewModelScope.launch {
            try {
                val climas = climaHistorial.value
                val finanzas = finanzasHistorial.value
                val telemetrias = dronesHistorial.value

                val reporte = AsistenteIA.consultarAsesorConConsulta(
                    consulta = consultaUsuarioIA,
                    climas = climas,
                    finanzas = finanzas,
                    telemetrias = telemetrias,
                    latitud = latitudCelular,
                    longitud = longitudCelular,
                    tempRealTime = tempRealTime,
                    humAireRealTime = humAireRealTime,
                    vientoRealTime = vientoRealTime,
                    humSueloRealTime = humSueloRealTime,
                    cotizacionDolarBlue = cotizacionDolarBlue,
                    cotizacionDolarOficial = cotizacionDolarOficial,
                    precioTrigoArs = precioTrigoArs,
                    precioSojaArs = precioSojaArs,
                    precioCarneArs = precioCarneArs,
                    precioManzanaArs = precioManzanaArs,
                    dronesActivos = dronesActivos,
                    deFilaUsuario = deFilaUsuario,
                    deColUsuario = deColUsuario,
                    altitudFilaCelular = altitudFilaCelular
                )
                respuestaIA = reporte
            } catch (e: Exception) {
                respuestaIA = "Error al intentar descifrar el reporte del asesor: ${e.localizedMessage}"
            } finally {
                cargandoIA = false
            }
        }
    }

    // --- INTEGRACIÓN: GPS + CLIMA REAL-TIME DE API OPEN-SOURCE ---

    fun obtenerUbicacionYClimaReal() {
        cargandoUbicacionGps = true
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager != null) {
                    val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (fineGranted || coarseGranted) {
                        val providers = locationManager.getProviders(true)
                        var bestLocation: Location? = null
                        for (provider in providers) {
                            val loc = locationManager.getLastKnownLocation(provider)
                            if (loc != null) {
                                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                                    bestLocation = loc
                                }
                            }
                        }

                        if (bestLocation != null) {
                            latitudCelular = bestLocation.latitude
                            longitudCelular = bestLocation.longitude
                            ubicacionObtenidaPorGps = true
                            mensajeEstadoClimaReal = "Ubicación satelital activa: [${String.format(java.util.Locale.US, "%.4f", latitudCelular)}, ${String.format(java.util.Locale.US, "%.4f", longitudCelular)}]"
                            
                            // Sincronizar nuestro paisaje 3D con las coordenadas GPS reales del operador
                            actualizarPosicionUsuarioEnAlivio(latitudCelular, longitudCelular)
                            
                            // Consultar el clima para el área por API
                            consultarClimaRealApi(latitudCelular, longitudCelular)
                        } else {
                            val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                LocationManager.NETWORK_PROVIDER
                            } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                LocationManager.GPS_PROVIDER
                            } else {
                                null
                            }

                            if (provider != null) {
                                locationManager.requestSingleUpdate(provider, object : LocationListener {
                                    override fun onLocationChanged(location: Location) {
                                        latitudCelular = location.latitude
                                        longitudCelular = location.longitude
                                        ubicacionObtenidaPorGps = true
                                        mensajeEstadoClimaReal = "Ubicación satelital actualizada: [${String.format(java.util.Locale.US, "%.4f", latitudCelular)}, ${String.format(java.util.Locale.US, "%.4f", longitudCelular)}]"
                                        
                                        actualizarPosicionUsuarioEnAlivio(latitudCelular, longitudCelular)
                                        viewModelScope.launch {
                                            consultarClimaRealApi(latitudCelular, longitudCelular)
                                        }
                                    }
                                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                                    override fun onProviderEnabled(provider: String) {}
                                    override fun onProviderDisabled(provider: String) {}
                                }, android.os.Looper.getMainLooper())
                            } else {
                                mensajeEstadoClimaReal = "Ningún sensor de GPS activo en el dispositivo. Usando coordenadas del lote."
                                consultarClimaRealApi(latitudCelular, longitudCelular)
                            }
                        }
                    } else {
                        mensajeEstadoClimaReal = "Permiso de GPS denegado en el teléfono. Mostrando Clima de base."
                        consultarClimaRealApi(latitudCelular, longitudCelular)
                    }
                } else {
                    mensajeEstadoClimaReal = "Módulo GPS no disponible. Mostrando Clima base."
                    consultarClimaRealApi(latitudCelular, longitudCelular)
                }
            } catch (e: Exception) {
                mensajeEstadoClimaReal = "Inconveniente al geolocalizar: ${e.localizedMessage}"
                consultarClimaRealApi(latitudCelular, longitudCelular)
            } finally {
                cargandoUbicacionGps = false
            }
        }
    }

    suspend fun consultarClimaRealApi(lat: Double, lng: Double) {
        climaCargandoRealTime = true
        climaErrorRealTime = null
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current=temperature_2m,relative_humidity_2m,wind_speed_10m"
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrEmpty()) {
                        val jsonObj = JSONObject(bodyStr)
                        val currentObj = jsonObj.optJSONObject("current")
                        if (currentObj != null) {
                            val temp = currentObj.optDouble("temperature_2m", Double.NaN)
                            val hum = currentObj.optDouble("relative_humidity_2m", Double.NaN)
                            val wind = currentObj.optDouble("wind_speed_10m", Double.NaN)

                            withContext(Dispatchers.Main) {
                                if (!temp.isNaN()) tempRealTime = temp
                                if (!hum.isNaN()) humAireRealTime = hum
                                if (!wind.isNaN()) vientoRealTime = wind

                                // El riego / humedad de suelo lo derivamos agronómicamente
                                humSueloRealTime = if (!hum.isNaN()) {
                                    (hum * 0.72 + 18).coerceIn(15.0, 95.0)
                                } else {
                                    48.0
                                }

                                // Registrar de forma automática en la base histórica de datos local
                                repository.agregarClima(
                                    temperatura = tempRealTime ?: 18.0,
                                    humedadSuelo = humSueloRealTime ?: 50.0,
                                    humedadAire = humAireRealTime ?: 60.0,
                                    viento = vientoRealTime ?: 12.0,
                                    origen = "Lote GPS [API Clima]"
                                )
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                climaErrorRealTime = "Formato meteorológico no esperado."
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        climaErrorRealTime = "API Clima no disponible (Código ${response.code})."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    climaErrorRealTime = "Falla de conexión clima: ${e.localizedMessage}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    climaCargandoRealTime = false
                }
            }
        }
    }

    // --- INTEGRACIÓN: ECONOMÍA COTIZACIONES EN TIEMPO REAL DESDE BLUELYTICS ---

    fun actualizarCotizacionesGanaderasGereales() {
        viewModelScope.launch {
            consultarCotizacionesEnTiempoReal()
        }
    }

    suspend fun consultarCotizacionesEnTiempoReal() {
        cargandoFinanzasApi = true
        errorFinanzasApi = null
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.bluelytics.com.ar/v2/latest"
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrEmpty()) {
                        val jsonObj = JSONObject(bodyStr)
                        val blueObj = jsonObj.optJSONObject("blue")
                        val oficialObj = jsonObj.optJSONObject("oficial")

                        if (blueObj != null) {
                            val blueValue = blueObj.optDouble("value_sell", 1220.0)
                            val oficialValue = oficialObj?.optDouble("value_sell", 940.0) ?: 940.0

                            withContext(Dispatchers.Main) {
                                cotizacionDolarBlue = blueValue
                                cotizacionDolarOficial = oficialValue

                                // Cotizaciones en Pesos basadas en dólares internacionales de referencia
                                precioTrigoArs = blueValue * 220.0  // ref USD 220 / Ton
                                precioSojaArs = blueValue * 415.0   // ref USD 415 / Ton
                                precioCarneArs = blueValue * 1.85   // ref USD 1.85 / Kg (Novillo vivo Cañuelas)
                                precioManzanaArs = blueValue * 0.85 // ref USD 0.85 / Kg de producción patagónica

                                // Sincroniza automáticamente los simuladores
                                precioVentaEstimado = precioManzanaArs.toInt().toString()
                                precioVentaGanado = (blueValue * 235.0).toInt().toString() // ref USD 235 por cabeza
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        errorFinanzasApi = "No se pudo obtener cotizaciones."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorFinanzasApi = "Conexión a finanzas fallida: ${e.localizedMessage}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    cargandoFinanzasApi = false
                }
            }
        }
    }

    // --- INTEGRACIÓN: TOPOGRAFÍA ACOPLADA AL GPS ---

    fun actualizarPosicionUsuarioEnAlivio(lat: Double, lng: Double) {
        // En base a la coordenada local centro de la chacra (-39.030, -67.580)
        // Mapeamos los desvíos GPS a los índices 0..6 de la matriz de relieve de 7x7
        val diffLat = lat - (-39.030)
        val diffLng = lng - (-67.580)

        // Una celda de 100m equivale aproximadamente a 0.0009 grados de diferencia
        val filaOffset = (diffLat / 0.0009).toInt()
        val colOffset = (diffLng / 0.0009).toInt()

        // Centramos y acotamos en el rango 0..6
        deFilaUsuario = (3 + filaOffset).coerceIn(0, 6)
        deColUsuario = (3 + colOffset).coerceIn(0, 6)

        // Asignamos altitud en base a la matriz de terreno
        altitudFilaCelular = matrizRelieve.getOrNull(deFilaUsuario)?.getOrNull(deColUsuario)?.toDouble() ?: 25.0
    }
}

/**
 * Representa la telemetría dinámica modelada para los drones corporativos DJI Agras T100 modulares.
 */
data class DronActivo(
    val id: String,
    val modelo: String,
    val bateria: Int,
    val nivelLiquido: Int,
    val capacidadMaxima: Int,
    val velocidad: Int,
    val tiempoVueloMinutos: Int,
    val actividad: String,
    val estado: String,
    val latitud: Double,
    val longitud: Double
)
