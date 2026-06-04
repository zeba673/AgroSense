<div align="center">
  <h1>🌱 AgroSense</h1>
  <p><strong>Gestión Inteligente de Chacras</strong></p>
  <p>
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
    <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
    <img src="https://img.shields.io/badge/Android-34A853?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
    <img src="https://img.shields.io/badge/OpenRouter-FF6B35?style=for-the-badge&logo=openai&logoColor=white" alt="OpenRouter"/>
    <img src="https://img.shields.io/badge/Gemini-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white" alt="Gemini AI"/>
  </p>
  <p>
    <img src="https://img.shields.io/github/license/zeba673/AgroSense?style=flat-square&color=2E7D32" alt="License"/>
    <img src="https://img.shields.io/github/last-commit/zeba673/AgroSense?style=flat-square&color=FF9800" alt="Last Commit"/>
    <img src="https://img.shields.io/github/repo-size/zeba673/AgroSense?style=flat-square&color=2196F3" alt="Repo Size"/>
    <img src="https://img.shields.io/badge/API-24%2B-brightgreen?style=flat-square" alt="Min SDK"/>
  </p>
</div>

---

## 📋 Descripción

**AgroSense** es una aplicación Android nativa diseñada para la gestión integral de establecimientos agropecuarios. Integra monitoreo climático en tiempo real, telemetría de drones DJI, control financiero, topografía 3D y un asistente de IA para recomendar las mejores decisiones agronómicas.

> 🧠 Generada desde **Google AI Studio** y potenciada con **Gemini API** + **OpenRouter**.

---

## 📸 Vista previa

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="https://via.placeholder.com/200x400/2E7D32/FFFFFF?text=Clima" alt="Clima" width="180"/>
        <br><sub>🌤️ Clima en vivo</sub>
      </td>
      <td align="center">
        <img src="https://via.placeholder.com/200x400/FF9800/FFFFFF?text=Drones" alt="Drones" width="180"/>
        <br><sub>🛸 Telemetría DJI</sub>
      </td>
      <td align="center">
        <img src="https://via.placeholder.com/200x400/2196F3/FFFFFF?text=Finanzas" alt="Finanzas" width="180"/>
        <br><sub>💰 Gestión económica</sub>
      </td>
      <td align="center">
        <img src="https://via.placeholder.com/200x400/8E75B2/FFFFFF?text=IA" alt="IA" width="180"/>
        <br><sub>🤖 Asistente IA</sub>
      </td>
    </tr>
  </table>
  <p><em>📱 Próximamente: capturas reales de la app</em></p>
</div>

---

## ✨ Features

### 🌤️ Clima en tiempo real
- Datos satelitales vía **Open-Meteo API** (sin API key requerida)
- Temperatura actual, humedad del aire y del suelo
- Velocidad y dirección del viento
- Registro automático con geolocalización GPS

### 🛸 Telemetría de drones DJI
- Monitoreo en vivo del nivel de batería de cada dron
- Control de nivel de líquido pulverizador y capacidad máxima
- Rutas de vuelo asignadas y estado operativo (fumigando / retornando / vigilando / cargando)
- Historial de telemetrías descargadas por cable USB

### 💰 Gestión económica
- Registro de ingresos y egresos por rubro y concepto
- Cálculo automático de balance neto y proyecciones
- Cotizaciones sincronizadas: dólar blue, dólar oficial
- Precios de commodities: trigo, soja, carne, manzana

### 🗺️ Topografía 3D
- Visualización del terreno con altitud y coordenadas GPS
- Matriz 3D de 7x7 cuadrantes para ubicación precisa del operador
- Datos de elevación digital del terreno (DEM)

### 🤖 Asistente IA (Gemini + OpenRouter)
- Consultas en lenguaje natural sobre el estado de la chacra
- Recomendaciones agronómicas personalizadas basadas en datos reales
- Análisis cruzado de clima + finanzas + telemetría de drones
- Modo offline con diagnósticos locales inteligentes de respaldo

### 🔒 Seguridad y privacidad
- Todos los datos sensibles cifrados localmente en el dispositivo
- Contraseñas almacenadas con hash SHA-256
- Las API keys se inyectan en tiempo de compilación via Secrets plugin
- Sin exposición de credenciales en el código fuente

---

## 🛠️ Tech Stack

| Capa | Tecnología |
|------|-----------|
| **Lenguaje** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Arquitectura** | MVVM + Repository Pattern |
| **Base de datos** | Room (SQLite) |
| **Red** | OkHttp + Retrofit |
| **Serialización** | Moshi |
| **IA** | Gemini API + OpenRouter (Gemini Flash) |
| **Inyección de secretos** | Secrets Gradle Plugin |
| **Testing** | JUnit, Robolectric, Roborazzi |
| **Compilación** | Gradle KTS + Kotlin DSL |

---

## 🧱 Arquitectura

```
┌─────────────────────────────────────────────┐
│                   UI Layer                   │
│  ┌─────────────┐  ┌──────────────────────┐  │
│  │  Pantallas   │  │     ViewModels       │  │
│  │  (Compose)   │◄─┤  (AgriViewModel)    │  │
│  └─────────────┘  └──────────┬───────────┘  │
├──────────────────────────────┼──────────────┤
│              Data Layer      │              │
│  ┌───────────────────────────┴───────────┐  │
│  │          Repository                   │  │
│  │  ┌──────────┐  ┌──────────────────┐  │  │
│  │  │  Room DB │  │  API (IA/Clima)  │  │  │
│  │  │ (SQLite) │  │  (Retrofit/OkH)  │  │  │
│  │  └──────────┘  └──────────────────┘  │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

---

## 🚀 Cómo ejecutar

### Prerrequisitos
- [Android Studio](https://developer.android.com/studio) (última versión estable)
- JDK 17+
- Dispositivo Android físico o emulador con API 24+

### Pasos

1. **Abrir el proyecto** en Android Studio
2. **Sincronizar Gradle** — Android Studio descargará las dependencias automáticamente
3. **Configurar API Keys** — Crear un archivo `.env` en la raíz del proyecto:
   ```env
   GEMINI_API_KEY=tu_api_key_aqui
   OPENROUTER_API_KEY=tu_api_key_aqui
   ```
4. **Ejecutar** → `Shift + F10` o botón Run

> 📌 Las API keys se inyectan automáticamente via el plugin de Secrets.  
> Obtén tu key de Gemini en [Google AI Studio](https://aistudio.google.com/) y tu key de OpenRouter en [openrouter.ai](https://openrouter.ai/).

---

## 📁 Estructura del proyecto

```
AgroSense/
├── app/
│   ├── src/
│   │   ├── main/java/com/example/
│   │   │   ├── api/              → Cliente IA (Gemini + OpenRouter)
│   │   │   ├── data/             → DAO, Database, Repository, Entidades
│   │   │   └── ui/               → ViewModel, Pantallas, Tema
│   │   ├── test/                 → Tests unitarios y screenshot tests
│   │   └── androidTest/          → Tests de instrumentación
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml        → Catálogo de versiones
├── .env.example                  → Template de API keys
├── build.gradle.kts              → Build raíz
├── settings.gradle.kts
├── gradle.properties
└── metadata.json
```

---

## 🗺️ Roadmap

- [x] Clima satelital en tiempo real
- [x] Gestión financiera con cotizaciones
- [x] Asistente IA con Gemini + OpenRouter
- [ ] Integración con drones DJI en vivo (RTK)
- [ ] Mapas de calor de rendimiento por lote
- [ ] Notificaciones push de alertas climáticas
- [ ] Exportación de reportes en PDF
- [ ] Sincronización cloud opcional
- [ ] Versión iOS (SwiftUI)

---

## 🤝 Contribuir

¿Tenés ideas para mejorar AgroSense? ¡Son bienvenidas!

1. Forkeá el proyecto
2. Creá una rama (`git checkout -b feature/mi-idea`)
3. Commiteá tus cambios (`git commit -m "Agrego mi idea"`)
4. Pusheá la rama (`git push origin feature/mi-idea`)
5. Abrí un Pull Request

---

## 📄 Licencia

Este proyecto se distribuye bajo los términos de la licencia MIT.  
Consultá el archivo `LICENSE` para más detalles.

---

<div align="center">
  <p>
    <a href="https://github.com/zeba673/AgroSense">
      <img src="https://img.shields.io/github/stars/zeba673/AgroSense?style=social" alt="Stars"/>
    </a>
    <a href="https://github.com/zeba673/AgroSense/fork">
      <img src="https://img.shields.io/github/forks/zeba673/AgroSense?style=social" alt="Forks"/>
    </a>
  </p>
  <p>Hecho con ❤️ para el campo argentino</p>
  <p>
    <a href="https://github.com/zeba673/AgroSense">GitHub</a> •
    <a href="https://ai.studio/apps/e40cce08-d800-4ebd-aacf-ea5d03de0f45">AI Studio</a>
  </p>
</div>
