package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.MedicionClima
import com.example.data.RegistroEconomia
import com.example.data.TelemetriaDron
import com.example.ui.DronActivo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente de consulta de Inteligencia Artificial para el Asesor Agrícola.
 * Cumple con la restricción de ser el único módulo con salida a Internet.
 * Procesa en hilos secundarios para mantener la UI ágil y fluida.
 * 
 * Integra el acceso directo a OpenRouter con el modelo Gemini Flash
 * para cruzamientos complejos y respuestas fundamentadas en la telemetría en tiempo real de la chacra.
 */
object AsistenteIA {

    private const val TAG = "AsistenteIA"
    private const val MODELO = "gemini-3.5-flash"

    // Configuración para OpenRouter
    private const val OPENROUTER_API_KEY = "YOUR_OPENROUTER_API_KEY"
    private const val OPENROUTER_MODELO = "google/gemini-2.5-flash"
    private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

    // OkHttpClient configurado con tiempos de espera holgados para evitar cortes de red
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    /**
     * Envía los datos locales de la chacra al modelo de IA y retorna la recomendación estratégica.
     */
    suspend fun consultarAsesor(
        climas: List<MedicionClima>,
        finanzas: List<RegistroEconomia>,
        telemetrias: List<TelemetriaDron>
    ): String = withContext(Dispatchers.IO) {

        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Verificar si la clave no está configurada o es el marcador de posición por defecto
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            Log.w(TAG, "Clave API de Gemini no válida o vacía. Generando asesoría inteligente local.")
            return@withContext generarAsesoriaLocalDemostrativa(climas, finanzas, telemetrias)
        }

        // Construir el compilado en texto plano de datos locales en español
        val resumenClima = climas.take(5).joinToString("\n") {
            "- Temperatura: ${it.temperatura}°C, Humedad de Suelo: ${it.humedadSuelo}%, Humedad de Aire: ${it.humedadAire}%, Viento: ${it.viento} km/h (Origen: ${it.origen})"
        }.ifEmpty { "No hay registros recientes." }

        val ingresos = finanzas.filter { it.tipo == "Ingreso" }.sumOf { it.monto }
        val egresos = finanzas.filter { it.tipo == "Egreso" }.sumOf { it.monto }
        val balance = ingresos - egresos
        val resumenFinanzas = finanzas.take(5).joinToString("\n") {
            "- ${it.tipo}: ${it.concepto} (${it.rubro}) - $${it.monto} por ${it.cantidad} unidad(es)"
        }.ifEmpty { "No hay movimientos recientes." }

        val resumenDrones = telemetrias.take(3).joinToString("\n") {
            "- Dron: ${it.identificadorDron}, Batería: ${it.bateria}%, Fluido: ${it.nivelLiquido}%, Ruta: ${it.rutaNombre} (${it.estadoVuelo})"
        }.ifEmpty { "No hay telemetrías registradas." }

        val prompt = SystemPromptBuilder(
            resumenClima = resumenClima,
            ingresos = ingresos,
            egresos = egresos,
            balance = balance,
            resumenFinanzas = resumenFinanzas,
            resumenDrones = resumenDrones
        )

        try {
            // URL de destino del servicio de lenguaje generativo
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODELO:generateContent?key=$apiKey"

            // Crear el cuerpo de la solicitud JSON usando org.json
            val jsonRequest = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObject = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObject = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObject)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObject)
                }
                put("contents", contentsArray)
                
                // Configurar temperatura baja para mayor certeza técnica
                val config = JSONObject().apply {
                    put("temperature", 0.3)
                }
                put("generationConfig", config)
            }

            val body = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "Error de Red: ${response.code} (Servidor de lenguaje no disponible). Cargando diagnóstico local temporal:\n\n" +
                        generarAsesoriaLocalDemostrativa(climas, finanzas, telemetrias)
            }

            val responseBody = response.body?.string() ?: return@withContext "Error: El servidor retornó una respuesta vacía."
            
            // Decodificar el JSON de respuesta
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidateObj = candidates.getJSONObject(0)
                val responseContent = candidateObj.optJSONObject("content")
                if (responseContent != null) {
                    val parts = responseContent.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No se pudo extraer el texto de la asesoría.")
                    }
                }
            }
            "La consulta se realizó correctamente pero el formato de respuesta del modelo fue inesperado. Intente nuevamente."
        } catch (e: Exception) {
            Log.e(TAG, "Error en la llamada de red a Gemini", e)
            "Error de Conectividad: ${e.localizedMessage ?: "No se pudo entablar la comunicación"}.\n\n" +
                    generarAsesoriaLocalDemostrativa(climas, finanzas, telemetrias)
        }
    }

    /**
     * Realiza una consulta personalizada al modelo de lenguaje mediante la pasarela de OpenRouter con la API de usuario.
     * Toma todos los datos dinámicos recopilados de sensores, GPS, drones activos y el mercado de finanzas
     * garantizándole al usuario una consejería de alta granularidad y precisión.
     */
    suspend fun consultarAsesorConConsulta(
        consulta: String,
        climas: List<MedicionClima>,
        finanzas: List<RegistroEconomia>,
        telemetrias: List<TelemetriaDron>,
        latitud: Double,
        longitud: Double,
        tempRealTime: Double?,
        humAireRealTime: Double?,
        vientoRealTime: Double?,
        humSueloRealTime: Double?,
        cotizacionDolarBlue: Double?,
        cotizacionDolarOficial: Double?,
        precioTrigoArs: Double,
        precioSojaArs: Double,
        precioCarneArs: Double,
        precioManzanaArs: Double,
        dronesActivos: List<DronActivo>,
        deFilaUsuario: Int,
        deColUsuario: Int,
        altitudFilaCelular: Double
    ): String = withContext(Dispatchers.IO) {

        // Construir resúmenes de históricos almacenados localmente
        val resumenClimaHistorial = climas.take(5).joinToString("\n") {
            "- Temperatura: ${it.temperatura}°C, Humedad de Suelo: ${it.humedadSuelo}%, Humedad de Aire: ${it.humedadAire}%, Viento: ${it.viento} km/h (Origen: ${it.origen})"
        }.ifEmpty { "Sin historial de mediciones climáticas locales." }

        val balanceHistorialText = finanzas.take(6).joinToString("\n") {
            "- ${it.tipo}: [${it.rubro}] ${it.concepto} -> $${String.format(java.util.Locale.US, "%,.1f", it.monto)} ARS por ${it.cantidad} unidad(es)"
        }.ifEmpty { "Sin movimientos financieros históricos registrados." }

        val resumenDronesHistorialText = telemetrias.take(5).joinToString("\n") {
            "- Conexión de Dron: ${it.identificadorDron}, Batería: ${it.bateria}%, Nivel de Fluido: ${it.nivelLiquido}%, Ruta: ${it.rutaNombre} (${it.estadoVuelo})"
        }.ifEmpty { "Sin descargas físicas de telemetría por cable USB registradas." }

        // Formatear flotas de drones volando en vivo
        val resumenDronesActivosText = dronesActivos.joinToString("\n") {
            "- Dron ${it.modelo} (${it.id}): Batería: ${it.bateria}%, Nivel Líquido: ${it.nivelLiquido} L / ${it.capacidadMaxima} L, Velocidad: ${it.velocidad} km/h, Tiempo Vuelo: ${it.tiempoVueloMinutos} min, Estado de Vuelo: ${it.estado} (Actividad: ${it.actividad}) [GPS: ${String.format(java.util.Locale.US, "%.5f", it.latitud)}, ${String.format(java.util.Locale.US, "%.5f", it.longitud)}]"
        }.ifEmpty { "No hay drones sobrevolando las parcelas del campo actualmente." }

        // Formatear clima de satélites / GPS en tiempo real
        val climasActualesText = if (tempRealTime != null) {
            "- Temperatura Atmosférica en Vivo: ${String.format(java.util.Locale.US, "%.1f", tempRealTime)}°C\n" +
            "- Humedad del Aire: ${String.format(java.util.Locale.US, "%.1f", humAireRealTime)}%\n" +
            "- Velocidad de Vientos Locales: ${String.format(java.util.Locale.US, "%.1f", vientoRealTime)} km/h\n" +
            "- Humedad Estimada de Suelo: ${String.format(java.util.Locale.US, "%.1f", humSueloRealTime)}% (Optimización Agronómica)"
        } else {
            "Sistemas meteorológicos GPS no sincronizados aún. (Presione el botón 'Consultar Clima Local (GPS)' en el módulo de clima)."
        }

        // Formatear precios del mercado y dólar en tiempo real
        val cotizacionesSincronizadasText = """
            - Dólar de Venta Blue Sincronizado: $${cotizacionDolarBlue ?: 1220.0} ARS
            - Dólar Oficial Sincronizado: $${cotizacionDolarOficial ?: 940.0} ARS
            - Commodity Trigo Rosario (FOB / Ton): $${String.format(java.util.Locale.US, "%,.0f", precioTrigoArs)} ARS
            - Commodity Soja Argentina (FOB / Ton): $${String.format(java.util.Locale.US, "%,.0f", precioSojaArs)} ARS
            - Ganado Vacuno Cañuelas (Kg Vivo Liniers): $${String.format(java.util.Locale.US, "%,.1f", precioCarneArs)} ARS/Kg
            - Cotización de Manzana Premium Real ARS (Kg): $${String.format(java.util.Locale.US, "%,.1f", precioManzanaArs)} ARS/Kg
        """.trimIndent()

        // Formatear posición física actual del productor
        val posicionTerrenoText = """
            - Ubicación Latitud Celular: ${String.format(java.util.Locale.US, "%.6f", latitud)}
            - Ubicación Longitud Celular: ${String.format(java.util.Locale.US, "%.6f", longitud)}
            - Altura Topográfica Digital (DEM): ${String.format(java.util.Locale.US, "%.1f", altitudFilaCelular)} metros sobre el nivel del mar
            - Cuadrante del Operador en Matriz 3D: Sector fila $deFilaUsuario, columna $deColUsuario
        """.trimIndent()

        val prompt = """
            Eres un asesor agrícola experto de primer nivel, especializado en administración de chacras latinoamericanas, monitoreo remoto, telemetría aérea de drones DJI Agras, relieve topográfico 3D y rentabilidad de lotes comerciales.
            Analiza meticulosamente la siguiente información de la chacra en tiempo real (recolectada dinámicamente) y responde la consulta que el usuario acaba de ingresar sobre su administración, de forma sabia, técnica y concreta:

            --- CONSULTA DIRECTA DEL USUARIO / OPERADOR DE LA CHACRA ---
            👉 "$consulta"

            =================== DATOS RECOLECTADOS EN TIEMPO REAL ===================

            📍 POSICIONAMIENTO SATELITAL GPS Y TOPOGRAFÍA 3D EN TERRENO:
            $posicionTerrenoText

            🌦️ CONDICIÓN CLIMÁTICA DE SATÉLITES EN VIVO EN LAS COORDENADAS DEL GPS:
            $climasActualesText

            📊 MERCADO DE FINANZAS Y COTIZACIONES EN ARS (DOLAR BLUE ACTUALIZADO):
            $cotizacionesSincronizadasText

            🛸 TELEMETRÍA EN TIEMPO REAL - DRONES EN VUELO (AEROPUERTOS LOCALES):
            $resumenDronesActivosText

            📚 HISTORIAL CRIPTO-CONTABLE Y METEOROLÓGICO DE LA CHACRA (ALMACENADO):
            * Últimas Mediciones de Sensores de Campo:
            $resumenClimaHistorial
            * Últimos Movimientos Económicos Registrados:
            $balanceHistorialText
            * Registro de Telemetrías Descargadas por Cable Físico USB:
            $resumenDronesHistorialText

            =================== REGLAS DE RESPUESTA EXIGIDAS ===================
            - Responde completamente en ESPAÑOL, usando un tono profesional, alentador y sumamente asertivo.
            - Estructura tu respuesta utilizando formato de viñetas (*), párrafos limpios y secciones diferenciadas para que el operador de campo pueda leer de un vistazo en la pantalla de su teléfono.
            - Utiliza de manera inteligente los datos financieros reales (precios de trigo, carne, manzanas), climáticos o el estado de batería de los drones DJI en vuelo para justificar científicamente tus respuestas y predicciones.
            - Evita usar términos en inglés redundantes (traduce "dashboard" a "panel de control", "AI advisor" a "consejo inteligente"). No digas que eres un modelo de IA ni expliques limitaciones de tu fecha. Actúa puramente como el Asesor de Élite del predio de fruta y ganado de la Patagonia.
        """.trimIndent()

        try {
            // Cuerpo de solicitud JSON con el estándar OpenRouter
            val jsonRequest = JSONObject().apply {
                put("model", OPENROUTER_MODELO)
                val messagesArray = JSONArray().apply {
                    val messageObject = JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }
                    put(messageObject)
                }
                put("messages", messagesArray)
                put("temperature", 0.3)
            }

            val body = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(OPENROUTER_URL)
                .addHeader("Authorization", "Bearer $OPENROUTER_API_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://ai.studio/build")
                .addHeader("X-Title", "Asistente Agrícola Satelital Inteligente")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "Error de Conectividad con la IA (Código ${response.code}). Mostrando análisis de respaldo local:\n\n" +
                        generarAsesoriaLocalDemostrativa(climas, finanzas, telemetrias)
            }

            val responseBody = response.body?.string() ?: return@withContext "Respuesta vacía recibida del canal de inteligencia."
            
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val choiceObj = choices.getJSONObject(0)
                val messageObj = choiceObj.optJSONObject("message")
                if (messageObj != null) {
                    val contentText = messageObj.optString("content", "")
                    if (contentText.isNotEmpty()) {
                        return@withContext contentText
                    }
                }
            }
            "No se pudo descifrar la respuesta analítica del modelo satelital. Por favor, reintente la consulta."
        } catch (e: Exception) {
            Log.e(TAG, "Error en llamada OpenRouter", e)
            "No posees conexión a internet o el canal de OpenRouter está saturado. ${e.localizedMessage}.\n\nGenerando análisis de protección local:\n\n" +
                    generarAsesoriaLocalDemostrativa(climas, finanzas, telemetrias)
        }
    }

    private fun SystemPromptBuilder(
        resumenClima: String,
        ingresos: Double,
        egresos: Double,
        balance: Double,
        resumenFinanzas: String,
        resumenDrones: String
    ): String {
        return """
            Eres un asesor agrícola experto de primer nivel, especializado en administración de chacras latinoamericanas, monitoreo remoto y rentabilidad de lotes. Analiza los siguientes datos recolectados localmente en este smartphone:

            --- REGISTROS CLIMÁTICOS LOCALES (ÚLTIMOS 5) ---
            $resumenClima

            --- BALANCE FINANCIERO RECIENTE ---
            * Ingresos Totales: ${'$'}$ingresos
            * Egresos Totales: ${'$'}$egresos
            * Balance Neto: ${'$'}$balance
            $resumenFinanzas

            --- TELEMETRÍA DE DRONES ASOCIADA ---
            $resumenDrones

            Escribe un diagnóstico agronómico estratégico y detallado en ESPAÑOL. Elabora exactamente tres secciones breves pero sustanciales usando viñetas (*):
            1. CLIMA Y RIEGO: Diagnóstico de la humedad del suelo e indiciones para riego o protección ante heladas/olas de calor basadas en los datos provistos.
            2. RECOMENDACIÓN DE OPERACIONES CON DRONES: Evaluar si el nivel de batería y líquido pulverizador reportados son suficientes para realizar misiones de fumigación eficaces sobre el lote perímetral.
            3. EVALUACIÓN DE PROYECCIÓN ECONÓMICA: Recomendaciones financieras para optimizar el balance que actualmente es de ${'$'}$balance. Indica si los insumos representan un egreso preocupante o si hay buen rendimiento.

            Evita usar términos en inglés, como "dashboard", "smart farming", o "AI advisory". Tradúcelos por "panel del campo", "agricultura inteligente", o "consejo inteligente". Usa un tono formal, alentador y profesional.
        """.trimIndent()
    }

    /**
     * Método de respaldo simulado inteligente para cuando el dispositivo no tiene internet
     * o la clave API no está ingresada en los secretos. Ejecuta lógica local e independiente.
     */
    private fun generarAsesoriaLocalDemostrativa(
        climas: List<MedicionClima>,
        finanzas: List<RegistroEconomia>,
        telemetrias: List<TelemetriaDron>
    ): String {
        val ultimoClima = climas.firstOrNull()
        val ultimaTelemetria = telemetrias.firstOrNull()
        val totalIngresos = finanzas.filter { it.tipo == "Ingreso" }.sumOf { it.monto }
        val totalEgresos = finanzas.filter { it.tipo == "Egreso" }.sumOf { it.monto }
        val neto = totalIngresos - totalEgresos

        // Análisis de clima local
        val diagnosticoClima = if (ultimoClima != null) {
            if (ultimoClima.humedadSuelo < 35.0) {
                "La humedad del suelo está críticamente baja (${ultimoClima.humedadSuelo}%). Se aconseja iniciar el ciclo de riego por goteo nocturno inmediatamente para conservar agua."
            } else if (ultimoClima.temperatura > 30.0) {
                "Temperatura elevada de ${ultimoClima.temperatura}°C. Vigile la evaporación del suelo para evitar resecamiento en el follaje central."
            } else {
                "Humedad del suelo estabilizada en ${ultimoClima.humedadSuelo}%. Monitoreo estándar óptimo para el desarrollo vegetativo de la chacra."
            }
        } else {
            "No se registran datos de sensores climáticos almacenados. Ingrese una medición manual de humedad y temperatura para generar su reporte."
        }

        // Análisis de drones local
        val diagnosticoDrones = if (ultimaTelemetria != null) {
            if (ultimaTelemetria.bateria < 20) {
                "El dron más reciente (${ultimaTelemetria.identificadorDron}) notificó batería baja del ${ultimaTelemetria.bateria}%. Se requiere cargar las celdas de litio antes del próximo vuelo."
            } else if (ultimaTelemetria.nivelLiquido < 15) {
                "El depósito pulverizador cuenta únicamente con ${ultimaTelemetria.nivelLiquido}% de producto. Abastezca el tanque principal previo a la ruta '${ultimaTelemetria.rutaNombre}'."
            } else {
                "El dron ${ultimaTelemetria.identificadorDron} reporta parámetros óptimos (Batería: ${ultimaTelemetria.bateria}%, Líquido: ${ultimaTelemetria.nivelLiquido}%). Listo para realizar vuelos de pulverizado en el lote asignado."
            }
        } else {
            "Aún no hay conexiones registradas con la base terrestre o cable de drones. Ingrese una telemetría local de vuelo para analizar la salud del sensor aéreo."
        }

        // Análisis de economía local
        val diagnosticoFinanzas = if (neto < 0) {
            "El balance total de la chacra presenta un déficit neto de $${-neto} pesos. Se recomienda analizar urgentemente los egresos correspondientes a insumos agroquímicos y postergar adquisiciones de maquinaria no prioritaria."
        } else {
            "El balance comercial reporta superávit positivo de +$${neto} pesos. Buen flujo de ingresos. Considere reinvertir en la automatización del riego o en mantenimiento preventivo de tractores."
        }

        return """
            [NOTA: Usando Motor de Análisis Local Independiente - Clave API no configurada o Sin Conexión]

            --- INFORME TÉCNICO Y RECOMENDACIÓN ESTRATÉGICA DE LA CHACRA ---

            * 1. RECOMENDACIÓN DE CLIMA Y SUELO:
              $diagnosticoClima

            * 2. OPERACIÓN DE DRONES PULVERIZADORES:
              $diagnosticoDrones

            * 3. DIRECTRICES ECONÓMICAS Y CONTROL FINANCIERO:
              $diagnosticoFinanzas

            --- SUGERENCIA DE MEJORA ---
            Para habilitar las funciones de sugerencias profundas extendidas de Inteligencia Artificial conectada, asegúrese de ingresar su CLAVE API de Gemini en el panel lateral de Secretos.
        """.trimIndent()
    }
}
