# Plan de Remediación de Seguridad: `Gamma_Mobile`

**Repositorio:** `KEVIN11000/Gamma_Mobile`  
**Severidad:** 🟡 **MEDIA (Exposición de Infraestructura)**  
**Componente:** Repositorio de Almacenamiento Seguro e Inyección de Dependencias de Red  
**Clasificación CWE:** CWE-200 (Exposure of Sensitive Information to an Unauthorized Actor)

---

## 1. Descripción del Hallazgo

En el código fuente de la aplicación Android (`SecureStorageRepositoryImpl.kt` y `NetworkModule.kt`), así como en la documentación interna y archivos de especificación (`docs/ESPECIFICACIONES_API_MOBILE.md`), se encuentra hardcodeada la URL base del servidor de producción:

```kotlin
// app/src/main/java/com/gamma/mobile/data/repository/SecureStorageRepositoryImpl.kt (Línea 55)
const val DEFAULT_BASE_URL = "https://kevin11000.pythonanywhere.com/api/v1/"
```
```kotlin
// app/src/main/java/com/gamma/mobile/di/NetworkModule.kt (Línea 90)
.baseUrl("https://kevin11000.pythonanywhere.com/api/v1/")
```

Adicionalmente, se documentan públicamente rutas internas del servidor que revelan nombres de archivos y endpoints sensibles, tales como:
* `https://kevin11000.pythonanywhere.com/api/v1/cierres/reportes/cierre_octubre_2026.pdf`
* `https://kevin11000.pythonanywhere.com/api/v1/webhooks/github`

---

## 2. Análisis de Riesgo

1. **Reconocimiento de Infraestructura (Reconnaissance):**  
   Al estar el repositorio público, cualquier bot de escaneo de internet (como Shodan, Censys o spiders de GitHub) asocia tu nombre de usuario y tu proyecto directamente con el subdominio `kevin11000.pythonanywhere.com`.
2. **Superficie de Ataque y Denegación de Servicio (DoS):**  
   Las cuentas de PythonAnywhere tienen límites estrictos de CPU por día ("CPU seconds"). Un atacante puede bombardear continuamente los endpoints descubiertos, agotando tu cuota diaria y dejando fuera de servicio tu bot y tu app móvil.
3. **Intentos de Fuerza Bruta:**  
   Al conocer las rutas exactas y el formato de cabecera (`X-API-KEY`), atacantes pueden lanzar diccionarios automatizados contra tu servidor.

---

## 3. Opciones de Remediación

Tienes dos caminos para resolver esta exposición:

### Opción A (Recomendada y más rápida): Cambiar el repositorio a Privado 🔒
Dado que `Gamma_Mobile` es tu aplicación móvil de uso personal/privado para gestionar tus finanzas y tareas:
1. Ve a tu repositorio en GitHub: **Settings**.
2. Desplázate hasta el final a la sección **Danger Zone**.
3. Haz clic en **Change repository visibility > Change to private**.
* **Beneficio:** Ocultas de inmediato el código, la URL del servidor, las especificaciones y los nombres de reportes de miradas externas, manteniendo la funcionalidad de compilación intacta.

---

### Opción B: Desacoplar la URL mediante `BuildConfig` (Si el repo debe seguir público)
Si deseas que el código siga siendo de código abierto, la URL de producción nunca debe estar en texto plano en el repositorio. Debe inyectarse localmente en tiempo de compilación.

#### 1. Modificar `app/build.gradle.kts`:
Lee la URL desde `local.properties` (archivo que ya está en el `.gitignore` y no se sube a GitHub):

```kotlin
// app/build.gradle.kts
val localProperties = java.util.Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val serverBaseUrl: String = localProperties.getProperty("GAMMA_API_BASE_URL") 
    ?: "https://api.gamma.example.com/api/v1/" // Placeholder genérico y seguro

android {
    defaultConfig {
        buildConfigField("String", "DEFAULT_BASE_URL", "\"$serverBaseUrl\"")
    }
    buildFeatures {
        buildConfig = true
    }
}
```

#### 2. Actualizar el código Kotlin:
```kotlin
// SecureStorageRepositoryImpl.kt
companion object {
    val DEFAULT_BASE_URL = BuildConfig.DEFAULT_BASE_URL
}
```

#### 3. Configurar tu `local.properties` en tu PC:
```properties
# local.properties (local en tu máquina, NO commitear)
GAMMA_API_BASE_URL=https://kevin11000.pythonanywhere.com/api/v1/
```
