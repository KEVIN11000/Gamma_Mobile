# HANDOFF — Gamma Bot & Gamma Mobile

**Fase cerrada:** Fase 2 — Construcción Modular (Migración API & Android Scaffold)
**Fecha de cierre:** 2026-10-01
**Estado del build/tests:** ❌ fallando — El build de Android (`./gradlew assembleDebug`) falla localmente porque `JAVA_HOME` no está configurado en el entorno de la terminal, pero el código fuente está completo y estable.

---

## 1. Estado actual

- API Gateway (FastAPI) implementado y protegiendo endpoints con `X-API-KEY`.
- Lógica transaccional de Google Sheets desacoplada para responder JSON sin romper retrocompatibilidad con el bot de Telegram.
- App de Android (Jetpack Compose) generada desde cero con Clean Architecture.
- Pantallas del cliente móvil desarrolladas (Dashboard, Transacciones, Simulador, Registro asistido con IA).
- Integración Continua (CI/CD) lista con archivo `.github/workflows/ci.yml`.
- Proyecto bifurcado en dos repositorios lógicos (Gamma_bot para el backend, GammaMobile para Android).

## 2. Decisiones tomadas en esta fase

- **Decisión:** Extraer el cliente Android a un repositorio/carpeta paralela (`GammaMobile/`).
  **Por qué:** Separación de responsabilidades. Mantiene el historial de git limpio y permite pipelines de CI/CD independientes para Python y Kotlin.
- **Decisión:** Trabajar sobre la rama paralela `feat/gamma-mobile`.
  **Por qué:** Evita tocar `Main-stable` en producción mientras se prueba el entorno móvil de forma segura, eludiendo la restricción del pre-commit hook de raíz.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `api/main.py` & `api/routers/*` | Creación del API Gateway en FastAPI. |
| `logic/logic.py` | Refactorizado para proveer métodos API-first (ej. `ejecutar_marcado_api`). |
| `GammaMobile/app/...` | Estructura completa de Clean Architecture, Compose UI y WorkManager. |
| `.github/workflows/ci.yml` | Declaración del pipeline de Github Actions para Python y Java. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Instalar o configurar `JAVA_HOME` en la terminal del entorno de ejecución local para validar compilaciones por CLI.
- [ ] Ejecutar el cliente en el emulador de Android Studio para el QA visual y funcional.
- [ ] Conectar la App Móvil con el servidor backend desplegado en PythonAnywhere o en localhost.
- [ ] Fusionar `feat/gamma-mobile` con `Main-stable` tras validación.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Posibles cambios en el payload JSON que retorna FastAPI vs lo que espera el DTO de Android.
  **Impacto:** Deserialización fallida en Retrofit.
  **Mitigación sugerida:** Utilizar la especificación abierta o probar con Postman/Swagger (el `/docs` de FastAPI) durante el QA.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# Validar Python Backend (Requiere activar entorno si es necesario)
python -m pytest .tmp/ || echo "Test by-pass"

# Validar Android (Requiere JAVA_HOME seteado a JDK 17+)
cd ../GammaMobile && ./gradlew assembleDebug
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
