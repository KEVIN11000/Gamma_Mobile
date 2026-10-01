# 📱 Gamma Mobile (Android App)

**Gamma Mobile** es el cliente nativo de Android diseñado para interactuar con el ecosistema contable e inteligente de **Gamma Bot**. Construido utilizando las metodologías más modernas de desarrollo Android, actúa como un front-end desacoplado (BFF) conectado a la API de FastAPI del servidor central.

---

## ✨ Características Principales

- **📊 Dashboard Financiero:** Visualización en tiempo real del flujo de caja, balance (Ingresos vs Gastos) y estado de IVA.
- **⚡ Libro Diario (Transacciones):** Historial reactivo con filtros rápidos (Chips) para auditar movimientos.
- **🤖 Registro Inteligente con Gemini 2.5:** Integración de procesamiento de lenguaje natural (NLP) para agendar obligaciones o registrar gastos complejos con una simple frase o dictado de voz.
- **🏦 Gestor de Obligaciones y Simulador:** Panel interactivo para amortizar deudas y simular préstamos contra tu margen libre mensual.
- **🔔 Sincronización Background (Offline-First):** Gracias a `WorkManager` y `Room DB`, la app encola transacciones si no hay red, y emite alarmas locales anticipadas (30, 7, 5, 3 y 1 días) para tus obligaciones contables sin consumir datos.

---

## 🛠 Stack Tecnológico y Arquitectura

El proyecto sigue los lineamientos de **Clean Architecture** (Capas: Domain, Data, Presentation).

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Inyección de Dependencias:** Hilt / Dagger
- **Capa de Red:** Retrofit2 + OkHttp (Autenticación via `X-API-KEY`)
- **Base de Datos Local (Cache):** Room Database
- **Background Tasks:** Android WorkManager
- **Construcción:** Gradle Kotlin DSL (`.kts`)

---

## 🚀 Cómo Empezar (Desarrollo Local)

1. Clonar el repositorio.
2. Abrir el proyecto en **Android Studio** (Koala o superior).
3. Esperar a que **Gradle Sync** finalice (descargará las dependencias necesarias).
4. **Configurar el API Key:**
   - En el archivo `local.properties`, puedes definir la URL del servidor backend (PythonAnywhere) y la llave de seguridad, si el proyecto lo requiere.
5. Ejecutar en el emulador (Run `app`).

---

## 🏗 CI/CD

El proyecto cuenta con integración continua preparada para GitHub Actions. En cada push a la rama principal, el pipeline en la nube compilará un APK de depuración usando `./gradlew assembleDebug` para asegurar que el código es estable.
