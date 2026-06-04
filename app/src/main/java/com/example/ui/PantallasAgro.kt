package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MedicionClima
import com.example.data.RegistroEconomia
import com.example.data.TelemetriaDron
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape

/**
 * Vista Raíz que decide si mostrar la Pantalla de Inicio de Sesión o el Tablero Principal.
 */
@Composable
fun RaizAgroApp(viewModel: AgriViewModel) {
    val usuario = viewModel.usuarioLogueado

    if (usuario == null) {
        PantallaLogin(viewModel)
    } else {
        PantallaEstructuraPrincipal(viewModel)
    }
}

/**
 * 1. Pantalla de Inicio de Sesión y Registro Totalmente Local.
 */
@Composable
fun PantallaLogin(viewModel: AgriViewModel) {
    var esModoRegistro by remember { mutableStateOf(false) }
    var esModoRecuperar by remember { mutableStateOf(false) }
    var inputDni by remember { mutableStateOf("") }
    var inputContrasegna by remember { mutableStateOf("") }
    var inputNombre by remember { mutableStateOf("") }
    var contrasegnaVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9), // Verde claro primaveral de alta visibilidad
                        Color(0xFFC8E6C9)  // Verde pradera vivo diurno
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .testTag("tarjeta_login"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logotipo de campo con icono nativo
                Icon(
                    imageVector = Icons.Default.Agriculture,
                    contentDescription = "Logo Gestión Agrícola",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = "AgroSense",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E2723),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Sistema de Gestión y Monitoreo Autónomo\nby Sebastián Bagli",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Divider(modifier = Modifier.padding(bottom = 20.dp))

                // Mensajes de Alerta/Estado
                if (viewModel.mensajeErrorAuth.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = viewModel.mensajeErrorAuth,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }

                if (viewModel.mensajeExitoAuth.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = viewModel.mensajeExitoAuth,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }

                // Campos comunes
                OutlinedTextField(
                    value = inputDni,
                    onValueChange = { inputDni = it.filter { char -> char.isDigit() } },
                    label = { Text("Ingrese DNI (Numérico)") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("input_dni"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (esModoRegistro && !esModoRecuperar) {
                    OutlinedTextField(
                        value = inputNombre,
                        onValueChange = { inputNombre = it },
                        label = { Text("Nombre Completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("input_nombre"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = inputContrasegna,
                    onValueChange = { inputContrasegna = it },
                    label = { Text(if (esModoRecuperar) "Nueva Contraseña" else "Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { contrasegnaVisible = !contrasegnaVisible }) {
                            Icon(
                                imageVector = if (contrasegnaVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (contrasegnaVisible) "Ocultar" else "Mostrar"
                            )
                        }
                    },
                    visualTransformation = if (contrasegnaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .testTag("input_contrasenia"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Botones de acción
                Button(
                    onClick = {
                        if (esModoRecuperar) {
                            viewModel.reestablecerContrasegnaLocal(inputDni, inputContrasegna)
                        } else if (esModoRegistro) {
                            viewModel.registrarUsuario(inputDni, inputNombre, inputContrasegna)
                        } else {
                            viewModel.iniciarSesion(inputDni, inputContrasegna)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("boton_acceder"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !viewModel.cargandoAuth,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    if (viewModel.cargandoAuth) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = if (esModoRecuperar) {
                                "ESTABLECER CLAVE LOCAL"
                            } else if (esModoRegistro) {
                                "REGISTRAR EN EL TELÉFONO"
                            } else {
                                "INGRESAR AL SISTEMA"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (esModoRecuperar) {
                    TextButton(
                        onClick = {
                            esModoRecuperar = false
                            viewModel.limpiarMensajesAuth() // Limpiar mensajes
                        },
                        modifier = Modifier.testTag("boton_volver_login")
                    ) {
                        Text(
                            text = "← Volver al Inicio de Sesión",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    TextButton(
                        onClick = {
                            esModoRegistro = !esModoRegistro
                            viewModel.limpiarMensajesAuth() // Reiniciar errores
                        },
                        modifier = Modifier.testTag("boton_cambiar_modo")
                    ) {
                        Text(
                            text = if (esModoRegistro) "¿Ya tienes cuenta? Iniciar Sesión" else "¿Primera vez? Regístrate de forma local",
                            color = Color(0xFF3E2723),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = {
                            esModoRecuperar = true
                            esModoRegistro = false
                            viewModel.limpiarMensajesAuth() // Reiniciar errores
                        },
                        modifier = Modifier.testTag("boton_olvide_contra")
                    ) {
                        Text(
                            text = "¿Problemas con la contraseña? Reestablecer aquí",
                            color = Color(0xFFC62828),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Indicación de la encriptación local en la parte inferior de la caja
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF3E2723),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Datos encriptados localmente con SHA-256 en SQLite",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

/**
 * Estructura de barra de navegación principal adaptable para móviles (Bottom Bar) y pantallas anchas.
 * Sigue la pauta de Diseño Adaptativo canónico para Android.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEstructuraPrincipal(viewModel: AgriViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AgroSense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.cerrarSesion() },
                        modifier = Modifier.testTag("boton_cerrar_sesion")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión pública", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    selected = viewModel.pestagnaSeleccionada == "tablero",
                    onClick = { viewModel.pestagnaSeleccionada = "tablero" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Tablero", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_tablero")
                )
                NavigationBarItem(
                    selected = viewModel.pestagnaSeleccionada == "clima",
                    onClick = { viewModel.pestagnaSeleccionada = "clima" },
                    icon = { Icon(Icons.Default.Thunderstorm, contentDescription = null) },
                    label = { Text("Clima", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_clima")
                )
                NavigationBarItem(
                    selected = viewModel.pestagnaSeleccionada == "drones",
                    onClick = { viewModel.pestagnaSeleccionada = "drones" },
                    icon = { Icon(Icons.Default.FlightTakeoff, contentDescription = null) },
                    label = { Text("Drones", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_drones")
                )
                NavigationBarItem(
                    selected = viewModel.pestagnaSeleccionada == "economia",
                    onClick = { viewModel.pestagnaSeleccionada = "economia" },
                    icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    label = { Text("Economía", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_economia")
                )
                NavigationBarItem(
                    selected = viewModel.pestagnaSeleccionada == "topografia",
                    onClick = { viewModel.pestagnaSeleccionada = "topografia" },
                    icon = { Icon(Icons.Default.Terrain, contentDescription = null) },
                    label = { Text("Relieve 3D", fontSize = 11.sp) },
                    modifier = Modifier.testTag("tab_topografia")
                )
            }
        }
    ) { paddingInterno ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInterno)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (viewModel.pestagnaSeleccionada) {
                "tablero" -> ModuloTableroDashboard(viewModel)
                "clima" -> ModuloClima(viewModel)
                "drones" -> ModuloDrones(viewModel)
                "economia" -> ModuloEconomiaProyecciones(viewModel)
                "topografia" -> ModuloTopografia(viewModel)
            }
        }
    }
}

/**
 * 2. Módulo de Tableros de Control (Unificación visual de datos locales).
 * Muestra gráficos personalizados dibujados interactivamente con Canvas y el Asesor de IA.
 */
@Composable
fun ModuloTableroDashboard(viewModel: AgriViewModel) {
    val climas by viewModel.climaHistorial.collectAsState()
    val finanzas by viewModel.finanzasHistorial.collectAsState()
    val telemetrias by viewModel.dronesHistorial.collectAsState()

    val ultimoClima = climas.firstOrNull()
    val ultimaTelemetria = telemetrias.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Cabecera Bienvenida
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hola, ${viewModel.usuarioLogueado?.nombre ?: "Productor"}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "Lote activo controlado localmente: DNI ${viewModel.usuarioLogueado?.dni}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        // Fila de Métricas Rápidas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tarjeta Humedad Rápida
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Suelo Lote Norte", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (ultimoClima != null) "${ultimoClima.humedadSuelo}%" else "Sin datos",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text("Humedad relativa", fontSize = 10.sp, color = Color.DarkGray)
                    }
                }

                // Tarjeta Dron
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Batería de Dron", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (ultimaTelemetria != null) "${ultimaTelemetria.bateria}%" else "Inactivo",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if ((ultimaTelemetria?.bateria ?: 100) < 30) Color.Red else Color(0xFF2E7D32)
                        )
                        Text(
                            text = ultimaTelemetria?.identificadorDron ?: "No conectado",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Gráfico Financiero de la Chacra (Ingresos vs Egresos Piloto)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FLUJO COMERCIAL RECIENTE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723)
                    )
                    Text("Comparativa local e independiente de transacciones", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dibujo nativo con Canvas de gráficos de barra
                    val ingresosTotal = finanzas.filter { it.tipo == "Ingreso" }.sumOf { it.monto }.toFloat()
                    val egresosTotal = finanzas.filter { it.tipo == "Egreso" }.sumOf { it.monto }.toFloat()
                    val balanceTotal = ingresosTotal - egresosTotal

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val anchoBarra = 90.dp.toPx()
                            val maximaAltura = size.height * 0.75f
                            val valorMaximo = maxOf(ingresosTotal, egresosTotal, 100000f)

                            // Coordenadas barras
                            val xIngresos = size.width / 4f - anchoBarra / 2f
                            val xEgresos = size.width * 3f / 4f - anchoBarra / 2f

                            val hIngresos = (ingresosTotal / valorMaximo) * maximaAltura
                            val hEgresos = (egresosTotal / valorMaximo) * maximaAltura

                            // Dibujar Línea de Guía Base
                            drawLine(
                                color = Color.LightGray,
                                start = Offset(20f, size.height - 20f),
                                end = Offset(size.width - 20f, size.height - 20f),
                                strokeWidth = 2f
                            )

                            // Barra Ingresos (Verde)
                            drawRoundRect(
                                color = Color(0xFF2E7D32),
                                topLeft = Offset(xIngresos, size.height - 20f - hIngresos),
                                size = androidx.compose.ui.geometry.Size(anchoBarra, hIngresos),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )

                            // Barra Egresos (Rojo)
                            drawRoundRect(
                                color = Color(0xFFC62828),
                                topLeft = Offset(xEgresos, size.height - 20f - hEgresos),
                                size = androidx.compose.ui.geometry.Size(anchoBarra, hEgresos),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                    }

                    // Etiquetas de los gráficos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ingresos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("$${ingresosTotal.toInt()}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Egresos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            Text("$${egresosTotal.toInt()}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "Balance General Neto: $${balanceTotal.toInt()} pesos",
                        fontWeight = FontWeight.Bold,
                        color = if (balanceTotal >= 0) Color(0xFF1B5E20) else Color(0xFFC62828),
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        // Panel Consultor de IA Agrícola (Único componente que consume internet bajo requerimiento)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, Color(0xFF2E7D32)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Asesor Inteligente Incorporado",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }

                    Text(
                        text = "Cruce analítico de datos recopilados (Economía, Clima, Drones) mediante conexión externa de Inteligencia Artificial.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.consultaUsuarioIA,
                        onValueChange = { viewModel.consultaUsuarioIA = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        label = { Text("Consulta personalizada o instrucción a la IA", fontSize = 12.sp) },
                        placeholder = { Text("Escribe tu consulta sobre clima local, drones o contabilidad de la chacra...", fontSize = 11.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        maxLines = 3,
                        singleLine = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = Color(0xFF1B5E20)
                        ),
                        trailingIcon = {
                            if (viewModel.consultaUsuarioIA.isNotEmpty()) {
                                IconButton(onClick = { viewModel.consultaUsuarioIA = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpiar"
                                    )
                                }
                            }
                        }
                    )

                    // Sugerencias rápidas de consulta
                    Text(
                        text = "Preguntas sugeridas (Toca para seleccionar):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Optimizar riego" to "¿Cómo optimizar el riego hoy considerando los datos climáticos en tiempo real y humedad del suelo?",
                            "Drones DJI Agras" to "¿Cuál es el diagnóstico de la flota de drones DJI Agras? ¿Tienen carga suficiente para pulverizar hoy?",
                            "Precios de venta" to "¿Me conviene usar la cotización de la manzana o el novillo para optimizar mi balance hoy?",
                            "Relieve e inclinación" to "¿Cómo influye mi altitud satelital de operador y relieve 3D en la humedad de los lotes?"
                        ).forEach { (display, fullQuery) ->
                            ElevatedButton(
                                onClick = { viewModel.consultaUsuarioIA = fullQuery },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = Color(0xFFE8F5E9),
                                    contentColor = Color(0xFF1B5E20)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(display, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.consultarAsesorIA() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("boton_consultar_ia"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !viewModel.cargandoIA
                    ) {
                        if (viewModel.cargandoIA) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Consultando Gemini Flash (OpenRouter)...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GENERAR INFORME RECOMENDATICIO", fontSize = 13.sp)
                        }
                    }

                    AnimatedVisibility(
                        visible = viewModel.respuestaIA.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            border = BorderStroke(1.dp, Color.LightGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RECOMENDACIONES OBTENIDAS:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = viewModel.respuestaIA,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            var inputJsonImport by remember { mutableStateOf("") }
            var mensajeResultadoImport by remember { mutableStateOf("") }
            var mostrarConfirmarBorrado by remember { mutableStateOf(false) }

            if (mostrarConfirmarBorrado) {
                AlertDialog(
                    onDismissRequest = { mostrarConfirmarBorrado = false },
                    title = { Text("¿Eliminar todos los datos piloto?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = { Text("Esta acción borrará de forma permanente todas las mediciones de clima, registros de finanzas y telemetrías cargadas por defecto para que puedas ingresar tus datos reales de la chacra.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.vaciarTablasYDatosDemostrativos()
                                mostrarConfirmarBorrado = false
                                mensajeResultadoImport = "Base de datos vaciada con éxito. Ahora el sistema está limpio."
                            }
                        ) {
                            Text("SÍ, VACIAR BASE DE DATOS", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarConfirmarBorrado = false }) {
                            Text("CANCELAR")
                        }
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enlazar y Cargar Datos Reales",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }

                    Text(
                        text = "Configura AgroSense con datos reales de tu campo. Puedes vaciar los valores de ejemplo y cargar tu propio lote de registros en formato JSON.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { mostrarConfirmarBorrado = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpiar Todo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                inputJsonImport = """{
  "clima": [
    { "temperatura": 16.4, "humedadSuelo": 34.5, "humedadAire": 58.0, "viento": 14.2, "origen": "Lote Manzanar Sur" },
    { "temperatura": 18.1, "humedadSuelo": 32.0, "humedadAire": 56.5, "viento": 12.0, "origen": "Lote Frutales Norte" }
  ],
  "economia": [
    { "tipo": "Ingreso", "concepto": "Venta Real de Manzana Red Delicious", "rubro": "Fruta", "monto": 720000.0, "cantidad": 1800.0 },
    { "tipo": "Egreso", "concepto": "Compra Fertilizante Nitrogenado", "rubro": "Insumos", "monto": 95000.0, "cantidad": 3.0 }
  ],
  "telemetria": [
    { "identificadorDron": "DJI Agras T40 - Real", "nivelLiquido": 90, "bateria": 95, "rutaNombre": "Pulverización Cuadro 4", "estadoVuelo": "Exitoso" }
  ]
}"""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Plantilla Real", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = inputJsonImport,
                        onValueChange = { inputJsonImport = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(vertical = 4.dp),
                        label = { Text("Estructura de Datos JSON Real", fontSize = 11.sp) },
                        placeholder = { Text("Pega aquí tu JSON con claves 'clima', 'economia' o 'telemetria'...", fontSize = 11.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2E7D32),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Button(
                        onClick = {
                            if (inputJsonImport.trim().isEmpty()) {
                                mensajeResultadoImport = "Por favor, ingresa o pega datos JSON válidos."
                            } else {
                                mensajeResultadoImport = viewModel.importarDatosEstiloJson(inputJsonImport)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PROCESAR E ENLAZAR DATOS REALES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (mensajeResultadoImport.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (mensajeResultadoImport.startsWith("Error")) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Text(
                                text = mensajeResultadoImport,
                                fontSize = 12.sp,
                                color = if (mensajeResultadoImport.startsWith("Error")) Color(0xFFC62828) else Color(0xFF2E7D32),
                                modifier = Modifier.padding(10.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Módulo Meteorológico / Clima.
 * Carga de sensores manuales de humedad y análisis históricos de vientos y temperaturas.
 */
@Composable
fun ModuloClima(viewModel: AgriViewModel) {
    val climas by viewModel.climaHistorial.collectAsState()

    var tempInput by remember { mutableStateOf("22.0") }
    var humSueloInput by remember { mutableStateOf("45.0") }
    var humAireInput by remember { mutableStateOf("50.0") }
    var vientoInput by remember { mutableStateOf("12.0") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.obtenerUbicacionYClimaReal()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TARJETA DE CLIMA GPS SATELITAL EN TIEMPO REAL (API ABIERTA)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                border = BorderStroke(1.dp, Color(0xFF81C784))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MONITOREO CLIMÁTICO SATELITAL (GPS)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        if (viewModel.climaCargandoRealTime || viewModel.cargandoUbicacionGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF2E7D32),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (viewModel.tempRealTime != null) Color(0xFF4CAF50) else Color.Gray,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = viewModel.mensajeEstadoClimaReal,
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Temp Real
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Temperatura", fontSize = 10.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (viewModel.tempRealTime != null) "${String.format(Locale.US, "%.1f", viewModel.tempRealTime)}°C" else "---",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3E2723)
                                )
                            }
                        }

                        // Humedad Suelo
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Hum. Suelo (Est.)", fontSize = 10.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (viewModel.humSueloRealTime != null) "${String.format(Locale.US, "%.0f", viewModel.humSueloRealTime)}%" else "---",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0288D1)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Humedad Aire
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Humedad Aire", fontSize = 10.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (viewModel.humAireRealTime != null) "${String.format(Locale.US, "%.0f", viewModel.humAireRealTime)}%" else "---",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        // Viento
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Velocidad Viento", fontSize = 10.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (viewModel.vientoRealTime != null) "${String.format(Locale.US, "%.1f", viewModel.vientoRealTime)} km/h" else "---",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF57C00)
                                )
                            }
                        }
                    }

                    if (viewModel.climaErrorRealTime != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.climaErrorRealTime ?: "",
                            fontSize = 11.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.obtenerUbicacionYClimaReal() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONSULTAR CLIMA LOCAL (GPS)", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "Ingreso de Variables Meteorológicas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )
            Text(
                text = "Recopile datos de los sensores mecánicos de la chacra y registre los valores de forma local para analizar alertas.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NUEVA LECTURA DE CHACRA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Input temperatura
                    Text("Temperatura del Aire (°C)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = tempInput.toFloatOrNull() ?: 15f,
                        onValueChange = { tempInput = String.format(Locale.US, "%.1f", it) },
                        valueRange = -5f..45f,
                        modifier = Modifier.testTag("slider_temp")
                    )
                    Text("$tempInput °C", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                    // Input humedad suelo
                    Text("Humedad del Suelo (%)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = humSueloInput.toFloatOrNull() ?: 30f,
                        onValueChange = { humSueloInput = String.format(Locale.US, "%.1f", it) },
                        valueRange = 0f..100f,
                        modifier = Modifier.testTag("slider_hum_suelo")
                    )
                    Text("$humSueloInput %", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                    // Input humedad aire
                    Text("Humedad del Aire (%)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = humAireInput.toFloatOrNull() ?: 50f,
                        onValueChange = { humAireInput = String.format(Locale.US, "%.1f", it) },
                        valueRange = 0f..100f
                    )
                    Text("$humAireInput %", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                    // Input velocidad viento
                    Text("Velocidad del Viento (km/h)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = vientoInput.toFloatOrNull() ?: 10f,
                        onValueChange = { vientoInput = String.format(Locale.US, "%.1f", it) },
                        valueRange = 0f..80f
                    )
                    Text("$vientoInput km/h", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                    Button(
                        onClick = {
                            val temp = tempInput.toDoubleOrNull() ?: 20.0
                            val humS = humSueloInput.toDoubleOrNull() ?: 45.0
                            val humA = humAireInput.toDoubleOrNull() ?: 50.0
                            val v = vientoInput.toDoubleOrNull() ?: 10.0
                            viewModel.registrarMedicionClimaManual(temp, humS, humA, v)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("boton_guardar_clima"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("GUARDAR REGISTRO CLIMÁTICO LOCAL")
                    }
                }
            }
        }

        item {
            Text(
                text = "Historial Cromático de Mediciones",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (climas.isEmpty()) {
            item {
                Text(
                    text = "No hay lecturas archivadas en la base local.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(climas) { medicion ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = medicion.origen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF1B5E20)
                            )
                            val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                            Text(
                                text = df.format(Date(medicion.fecha)),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Temp: ${medicion.temperatura}°C", fontSize = 11.sp)
                            Text("Hum. Suelo: ${medicion.humedadSuelo}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("Viento: ${medicion.viento} km/h", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Módulo de Drones Agrícolas.
 * Telemetría activa en tiempo real para drones DJI Agras T100 y Mavic Multispectral de pulverización y topografía.
 */
@Composable
fun ModuloDrones(viewModel: AgriViewModel) {
    val telemetrias by viewModel.dronesHistorial.collectAsState()
    val ultima = telemetrias.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Telemetría Activa Corporativa DJI",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )
            Text(
                text = "Rastreo por radio local y satélite de los drones DJI Agras T100 (pulverizadores) y Mavic 3 (multiespectrales y topografía). Monitoree actividades en tiempo real sin salir de la cabina.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        // SECCIÓN CLAVE: FLOTA DE DRONES DJI EN TIEMPO REAL
        item {
            Text(
                text = "DRONES ACTIVOS EN VUELO",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF2E7D32)
            )
        }

        items(viewModel.dronesActivos) { dron ->
            val colorEstado = when(dron.estado) {
                "FUMIGANDO" -> Color(0xFF2E7D32)
                "RETORNANDO" -> Color(0xFFE65100)
                "VIGILANDO" -> Color(0xFF0288D1)
                else -> Color.Gray
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.5.dp, colorEstado.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                tint = colorEstado,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = dron.modelo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF3E2723)
                                )
                                Text(
                                    text = dron.id,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = colorEstado.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(color = colorEstado, shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = dron.estado,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorEstado
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = dron.actividad,
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Batería
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Batería: ${dron.bateria}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dron.bateria < 30) Color.Red else Color.DarkGray
                        )
                    }
                    LinearProgressIndicator(
                        progress = dron.bateria / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (dron.bateria < 30) Color.Red else Color(0xFF4CAF50),
                        trackColor = Color.LightGray.copy(alpha = 0.3f)
                    )

                    // Líquido (si el dron es un Agras de fumigación)
                    if (dron.capacidadMaxima > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Depósito Químico: ${dron.nivelLiquido} L / ${dron.capacidadMaxima} L",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0288D1)
                            )
                        }
                        LinearProgressIndicator(
                            progress = dron.nivelLiquido / dron.capacidadMaxima.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF03A9F4),
                            trackColor = Color.LightGray.copy(alpha = 0.3f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Telemetría de vuelo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Velocidad: ${dron.velocidad} km/h",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Tiempo de Vuelo: ${dron.tiempoVueloMinutos} min",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "GPS: [${String.format(Locale.US, "%.4f", dron.latitud)}, ${String.format(Locale.US, "%.4f", dron.longitud)}]",
                            fontSize = 10.sp,
                            color = Color(0xFF5D4037),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // CONEXIÓN FÍSICA PARA DESCARGA DE HISTORIAL
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ARCHIVO HISTÓRICO LOCAL",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DESCARGA DE DATOS POR CABLE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Permite registrar un informe estático oficial en la base criptográfica local a partir del puerto USB del radiocontrol DJI.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Indicador circular gráfico con Canvas para nivel de la última lectura descargada
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val bat = ultima?.bateria?.toFloat() ?: 0f
                            val liq = ultima?.nivelLiquido?.toFloat() ?: 0f

                            // Dibuja círculo de batería (Arco exterior verde)
                            drawArc(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(10f, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = if (bat < 30) Color.Red else Color(0xFF4CAF50),
                                startAngle = 135f,
                                sweepAngle = (bat / 100f) * 270f,
                                useCenter = false,
                                style = Stroke(10f, cap = StrokeCap.Round)
                            )

                            // Dibuja círculo de líquido (Arco interior celeste)
                            drawArc(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(10f, cap = StrokeCap.Round),
                                size = size * 0.7f,
                                topLeft = Offset(size.width * 0.15f, size.height * 0.15f)
                            )
                            drawArc(
                                color = Color(0xFF03A9F4),
                                startAngle = 135f,
                                sweepAngle = (liq / 100f) * 270f,
                                useCenter = false,
                                style = Stroke(10f, cap = StrokeCap.Round),
                                size = size * 0.7f,
                                topLeft = Offset(size.width * 0.15f, size.height * 0.15f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (ultima != null) "${ultima.bateria}% Bat" else "0% Bat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (ultima != null) "${ultima.nivelLiquido}% Líq" else "0% Líq",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0288D1),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.simularImportacionDeDronCable() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("boton_importar_telemetria"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Power, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("IMPORTAR TELEMETRÍA POR CABLE FISICO", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "Registros de Vuelos Archivados",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (telemetrias.isEmpty()) {
            item {
                Text(
                    text = "Aun no se han descargado registros de telemetría de vuelo.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(telemetrias) { itemDron ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = itemDron.identificadorDron,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3E2723),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Batería: ${itemDron.bateria}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (itemDron.bateria < 30) Color.Red else Color(0xFF1B5E20)
                            )
                        }
                        Text(
                            text = "Trayecto: ${itemDron.rutaNombre}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Líquido Pulverizado: ${itemDron.nivelLiquido} L",
                                fontSize = 11.sp,
                                color = Color(0xFF0288D1)
                            )
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (itemDron.estadoVuelo == "Exitoso") Color(0xFFC8E6C9) else Color(0xFFFFCDD2)
                                )
                            ) {
                                Text(
                                    text = itemDron.estadoVuelo,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (itemDron.estadoVuelo == "Exitoso") Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Módulo de Economía, Proyecciones y Rendimientos.
 * Calculadora matemática integrada y formulario de egresos/ingresos con persistencia en el dispositivo.
 */
@Composable
fun ModuloEconomiaProyecciones(viewModel: AgriViewModel) {
    val finanzas by viewModel.finanzasHistorial.collectAsState()

    var conceptoInput by remember { mutableStateOf("") }
    var montoInput by remember { mutableStateOf("") }
    var cantidadInput by remember { mutableStateOf("1") }
    var seleccionadoTipo by remember { mutableStateOf("Egreso") } // Ingreso o Egreso
    var seleccionadoRubro by remember { mutableStateOf("Insumos") } // Insumos, Ganado, Fruta, Maquinaria, Otros

    // Variables de Proyección Matemática
    val costoHa = viewModel.costoPorHectarea.toDoubleOrNull() ?: 0.0
    val haSel = viewModel.hectareasCultivo.toDoubleOrNull() ?: 0.0
    val precioFruta = viewModel.precioVentaEstimado.toDoubleOrNull() ?: 0.0
    val rindeKilos = viewModel.rendimientoKilosPorHectarea.toDoubleOrNull() ?: 0.0

    val precioAnimal = viewModel.precioCompraGanado.toDoubleOrNull() ?: 0.0
    val cabezas = viewModel.cantidadCabezas.toDoubleOrNull() ?: 0.0
    val ventaAnimal = viewModel.precioVentaGanado.toDoubleOrNull() ?: 0.0

    // Cálculo agrícola (fruta)
    val costoFrutaTotal = costoHa * haSel
    val produccionKilosTotal = rindeKilos * haSel
    val ingresosFrutaTotal = produccionKilosTotal * precioFruta
    val margenFruta = ingresosFrutaTotal - costoFrutaTotal

    // Cálculo pecuario (ganado)
    val costoGanadoTotal = precioAnimal * cabezas
    val ingresosGanadoTotal = ventaAnimal * cabezas
    val margenGanado = ingresosGanadoTotal - costoGanadoTotal

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Modelos Económicos de Chacra",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )
            Text(
                text = "Utilice los simuladores locales para ponderar la rentabilidad futura de sus frutales y cabezas de ganado, además de registrar cuentas actuales.",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        // CARD DE COTIZACIONES EN TIEMPO REAL (ARGENTINA / INTERNACIONAL)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                border = BorderStroke(1.5.dp, Color(0xFF9CCC65))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF33691E)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MERCADO EN TIEMPO REAL (API)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF33691E)
                            )
                        }
                        if (viewModel.cargandoFinanzasApi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF33691E),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Dólar Sell Sincronizado",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewModel.errorFinanzasApi != null) {
                        Text(
                            text = viewModel.errorFinanzasApi ?: "",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Fila de Dólar Oficial y Dólar Blue Argentina
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DÓLAR BLUE (VENTA)", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (viewModel.cotizacionDolarBlue != null) "$${viewModel.cotizacionDolarBlue?.toInt()} ARS" else "$1220 ARS",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DÓLAR OFICIAL", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (viewModel.cotizacionDolarOficial != null) "$${viewModel.cotizacionDolarOficial?.toInt()} ARS" else "$940 ARS",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "COTIZACIONES AGROPECUARIAS ESTIMADAS EN PESOS (ARS):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🌾 Trigo Rosario (FOB / Ton):", fontSize = 11.sp, color = Color.DarkGray)
                            Text("$${String.format(Locale.getDefault(), "%,.0f", viewModel.precioTrigoArs)} ARS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🌱 Soja Argentina (FOB / Ton):", fontSize = 11.sp, color = Color.DarkGray)
                            Text("$${String.format(Locale.getDefault(), "%,.0f", viewModel.precioSojaArs)} ARS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🥩 Novillo Cañuelas (Kg vivo):", fontSize = 11.sp, color = Color.DarkGray)
                            Text("$${String.format(Locale.getDefault(), "%,.1f", viewModel.precioCarneArs)} ARS/Kg", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🍎 Manzana Patagónica (Fruta Kg):", fontSize = 11.sp, color = Color.DarkGray)
                            Text("$${String.format(Locale.getDefault(), "%,.1f", viewModel.precioManzanaArs)} ARS/Kg", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.precioVentaEstimado = viewModel.precioManzanaArs.toInt().toString()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF558B2F)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.5f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Usar Manzana", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.precioVentaGanado = (viewModel.precioCarneArs * 150).toInt().toString() // ref 150 kilos media res por cabeza
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.5f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Usar Novillo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.actualizarCotizacionesGanaderasGereales() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33691E)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Actualizar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // CALCULADORA DE PROYECCIONES AGRÍCOLAS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2E7D32))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PROYECCIÓN MATEMÁTICA FRUTÍCOLA", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = viewModel.costoPorHectarea,
                        onValueChange = { viewModel.costoPorHectarea = it },
                        label = { Text("Costo Operativo por Hectárea ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("input_costo_ha")
                    )

                    OutlinedTextField(
                        value = viewModel.hectareasCultivo,
                        onValueChange = { viewModel.hectareasCultivo = it },
                        label = { Text("Hectáreas Cultivadas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.precioVentaEstimado,
                        onValueChange = { viewModel.precioVentaEstimado = it },
                        label = { Text("Precio de Venta Esperado (por Kilogramo)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.rendimientoKilosPorHectarea,
                        onValueChange = { viewModel.rendimientoKilosPorHectarea = it },
                        label = { Text("Rendimiento Esperado (Kilos por Hectárea)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Costo Producción Total: $${costoFrutaTotal.toInt()} pesos", fontSize = 12.sp)
                            Text("Volumen de Cosecha: ${produccionKilosTotal.toInt()} kilos", fontSize = 12.sp)
                            Text("Ingresos Brutos Estimados: $${ingresosFrutaTotal.toInt()} pesos", fontSize = 12.sp)
                            Text(
                                text = "MARGEN RENDIMIENTO NETO: $${margenFruta.toInt()} pesos",
                                fontWeight = FontWeight.Bold,
                                color = if (margenFruta >= 0) Color(0xFF1B5E20) else Color.Red,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // SIMULADOR PECUARIO (GANADO)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF1976D2))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Pets, contentDescription = null, tint = Color(0xFF1976D2))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PROYECCIÓN MATEMÁTICA PECUARIA", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = viewModel.precioCompraGanado,
                        onValueChange = { viewModel.precioCompraGanado = it },
                        label = { Text("Costo Adquisición Animal por Cabeza ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.cantidadCabezas,
                        onValueChange = { viewModel.cantidadCabezas = it },
                        label = { Text("Cabezas de Ganado Acopiadas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.precioVentaGanado,
                        onValueChange = { viewModel.precioVentaGanado = it },
                        label = { Text("Precio de Venta Esperado por Cabeza ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Inversión Ganadera Total: $${costoGanadoTotal.toInt()} pesos", fontSize = 12.sp)
                            Text("Ingresos Venta Total: $${ingresosGanadoTotal.toInt()} pesos", fontSize = 12.sp)
                            Text(
                                text = "MARGEN PECUARIO NETO: $${margenGanado.toInt()} pesos",
                                fontWeight = FontWeight.Bold,
                                color = if (margenGanado >= 0) Color(0xFF0D47A1) else Color.Red,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // REGISTRAR TRANSACCIÓN EN BASE DE DATOS LOCAL
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "REGISTRAR MOVIMIENTO CONTABLE ACTUAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { seleccionadoTipo = "Egreso" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (seleccionadoTipo == "Egreso") Color(0xFFC62828) else Color.LightGray
                            )
                        ) {
                            Text("Egreso (Gasto)")
                        }
                        Button(
                            onClick = { seleccionadoTipo = "Ingreso" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (seleccionadoTipo == "Ingreso") Color(0xFF2E7D32) else Color.LightGray
                            )
                        ) {
                            Text("Ingreso")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = conceptoInput,
                        onValueChange = { conceptoInput = it },
                        label = { Text("Concepto del Movimiento (Ej. Compra de Fertilizante)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("input_concepto_finanza")
                    )

                    OutlinedTextField(
                        value = montoInput,
                        onValueChange = { montoInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Monto en Pesos ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("input_monto_finanza")
                    )

                    OutlinedTextField(
                        value = cantidadInput,
                        onValueChange = { cantidadInput = it },
                        label = { Text("Cantidad asociada") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = {
                            val monto = montoInput.toDoubleOrNull() ?: 0.0
                            val cant = cantidadInput.toDoubleOrNull() ?: 1.0
                            if (conceptoInput.isNotEmpty() && monto > 0.0) {
                                viewModel.registrarGastoIngresoForm(
                                    tipo = seleccionadoTipo,
                                    concepto = conceptoInput,
                                    rubro = seleccionadoRubro,
                                    monto = monto,
                                    cantidad = cant
                                )
                                conceptoInput = ""
                                montoInput = ""
                                cantidadInput = "1"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("boton_guardar_finanzas"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("REGISTRAR EN CONTABILIDAD LOCAL")
                    }
                }
            }
        }
    }
}

/**
 * 6. Visor Topográfico Interactivo 3D en Compose Canvas.
 * Renderiza mapas de relieve tridimensionales del terreno de la chacra.
 * Responde a arrastres táctiles para rotar y deslizar el cabeceo del modelo.
 */
@Composable
fun ModuloTopografia(viewModel: AgriViewModel) {
    Text(
        text = "Visor Topográfico de Relieve",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF3E2723),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp)
    )

    Text(
        text = "Visualización tridimensional del relieve de la chacra para detectar desniveles de suelo que afecten la distribución del agua.",
        fontSize = 11.sp,
        color = Color.Gray,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Lienzo para el Renderizado de Mallas 3D
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEDF2F4)) // Fondo claro de alto contraste optimizado para el día en el campo
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            viewModel.inclinacionX += dragAmount.x * 0.4f
                            viewModel.inclinacionY += dragAmount.y * 0.4f
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Dibujar la malla del terreno mediante el Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centroX = size.width / 2f
                    val centroY = size.height / 2f

                    // Grados de rotación a Radianes
                    val radX = Math.toRadians(viewModel.inclinacionY.toDouble())
                    val radY = Math.toRadians(viewModel.inclinacionX.toDouble())

                    val cosX = cos(radX).toFloat()
                    val sinX = sin(radX).toFloat()
                    val cosY = cos(radY).toFloat()
                    val sinY = sin(radY).toFloat()

                    val matrizLen = viewModel.matrizRelieve.size
                    val factorEscala = 25f // Escala de tamaño para acomodar en pantalla
                    val factorAlto = viewModel.multiplicadorElevacion

                    // Almacenar coordenadas de proyección bidimensional
                    val puntosProyectados = Array(matrizLen) { Array<Offset?>(matrizLen) { null } }

                    for (i in 0 until matrizLen) {
                        for (j in 0 until matrizLen) {
                            // Centrar coordenadas locales (i, j) en el origen del Canvas (-3 a 3 para 7x7)
                            val localX = (i - 3) * factorEscala
                            val localY = (j - 3) * factorEscala
                            val localZ = viewModel.matrizRelieve[i][j] * factorAlto * 1.5f

                            // Matriz de Rotación 3D para renderizado isométrico interactivo
                            // 1. Rotación alrededor de eje Y (Giro horizontal)
                            val rotY_X = localX * cosY - localZ * sinY
                            val rotY_Z = localX * sinY + localZ * cosY

                            // 2. Rotación alrededor de eje X (Inclinación cabeceo)
                            val rotX_Y = localY * cosX - rotY_Z * sinX

                            // Proyección bidimensional paralela
                            puntosProyectados[i][j] = Offset(
                                x = rotY_X + centroX,
                                y = rotX_Y + centroY
                            )
                        }
                    }

                    // Dibujar líneas de cuadrícula para formar la malla tridimensional
                    for (i in 0 until matrizLen) {
                        for (j in 0 until matrizLen) {
                            val puntoBase = puntosProyectados[i][j] ?: continue

                            // Clasificación cromática por nivel de altura isobárica
                            val altura = viewModel.matrizRelieve[i][j]
                            val pinColor = when {
                                altura < 10f -> Color(0xFF4CAF50) // Valle fértil (Verde)
                                altura < 25f -> Color(0xFFD4E157) // Ladera (Verde claro)
                                altura < 35f -> Color(0xFFFFA726) // Cima de tierra (Marrón/Naranja)
                                else -> Color(0xFFECEFF1) // Pico rocoso (Blanco satura)
                            }

                            // Línea conectora hacia i + 1 (Fila)
                            if (i + 1 < matrizLen) {
                                val puntoFila = puntosProyectados[i + 1][j]
                                if (puntoFila != null) {
                                    drawLine(
                                        color = pinColor.copy(alpha = 0.6f),
                                        start = puntoBase,
                                        end = puntoFila,
                                        strokeWidth = 3f
                                    )
                                }
                            }

                            // Línea conectora hacia j + 1 (Columna)
                            if (j + 1 < matrizLen) {
                                val puntoColumna = puntosProyectados[i][j + 1]
                                if (puntoColumna != null) {
                                    drawLine(
                                        color = pinColor.copy(alpha = 0.6f),
                                        start = puntoBase,
                                        end = puntoColumna,
                                        strokeWidth = 3f
                                    )
                                }
                            }

                            // Dibujar punto de medición isométrico
                            drawCircle(
                                color = pinColor,
                                radius = 4f,
                                center = puntoBase
                            )

                            // Si este punto coincide con la fila/columna calculada por el GPS del usuario, se renderiza un pin de posición 3D real-time para el Operador
                            if (i == viewModel.deFilaUsuario && j == viewModel.deColUsuario) {
                                drawCircle(
                                    color = Color(0xFF2979FF).copy(alpha = 0.4f),
                                    radius = 16f,
                                    center = puntoBase
                                )
                                drawCircle(
                                    color = Color(0xFF2979FF),
                                    radius = 7f,
                                    center = puntoBase
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 3f,
                                    center = puntoBase
                                )
                            }
                        }
                    }
                }

                // Indicador de rotación en pantalla
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Rotar: Arrastre el dedo en el lóbulo central",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                    Text(
                        text = "Inclinación: X:${viewModel.inclinacionX.toInt()}° Y:${viewModel.inclinacionY.toInt()}°",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
        }

        // CARD DE UBICACIÓN SATELITAL DEL OPERADOR EN EL MODELO 3D
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            border = BorderStroke(1.dp, Color(0xFF90CAF9))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = Color(0xFF1565C0)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "POSICIONAMIENTO EN TERRENO 3D",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF1565C0)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "El modelo 3D se desplaza y enfoca automáticamente donde se encuentra usted caminando físicamente por la chacra con el dispositivo móvil, permitiéndole evaluar desniveles in-situ.",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Coordenadas GPS Actuales:",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        val la = viewModel.latitudCelular
                        val lo = viewModel.longitudCelular
                        Text(
                            text = if (la != -39.030) "${String.format(Locale.US, "%.5f", la)} , ${String.format(Locale.US, "%.5f", lo)}" else "Predeterminada: [${la} , ${lo}]",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E3B4E)
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text("Altitud GPS", fontSize = 8.sp, color = Color.Gray)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", viewModel.altitudFilaCelular)} m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fila/Columna de enfoque en la matriz 7x7
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Foco en Matriz DEM: Cuadrante [Fila ${viewModel.deFilaUsuario} , Columna ${viewModel.deColUsuario}]",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )

                    Button(
                        onClick = { viewModel.obtenerUbicacionYClimaReal() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Centrar GPS", fontSize = 10.sp)
                    }
                }
            }
        }

        // CONTROLADORES DE ESCALA Y SIMULACIÓN DE DISCO LOCAL DEM
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ajustar Exageración Vertical",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = viewModel.multiplicadorElevacion,
                    onValueChange = { viewModel.multiplicadorElevacion = it },
                    valueRange = 0.1f..3.0f,
                    modifier = Modifier.testTag("slider_altura")
                )
                Text(
                    text = "Factor de Elevación del Terreno: ${String.format(Locale.US, "%.1f", viewModel.multiplicadorElevacion)}x",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.simularCargaArchivoTopograficoDEM() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("boton_cargar_dem"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("IMPORTAR ARCHIVO .DEM (VISTA RELIEVE)")
                }
            }
        }
    }
}
