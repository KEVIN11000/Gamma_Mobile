# HANDOFF — Gamma Mobile

**Fase cerrada:** Fase 0 y Fase S (Especificación y Arquitectura, Especificación de Backend)
**Fecha de cierre:** 2026-10-05
**Estado del build/tests:** ❌ fallando — El comando `./gradlew assembleDebug` falla porque `JAVA_HOME` no está configurado en la terminal actual.

---

## 1. Estado actual
- Fase 0 (Especificación y Arquitectura) completada: `spec.md`, `plan.md` y `tasks.md` creados y ajustados a restricciones de servidor de 1 worker WSGI y usuario único.
- Fase S (Especificación de backend) completada: Generada carpeta `backend-spec/` con `openapi.yaml`, `REQUISITOS.md`, fixtures y mock server en Flask.
- Generado archivo de traspaso `HANDOFF-backend.md` para implementar la API real en `Gamma_bot`.
- Proyecto Android inicializado con Gradle wrapper y estructura base. Tareas S-01 a S-04 y M-01 a M-01 (E-01) marcadas como completadas.

## 2. Decisiones tomadas en esta fase
- **Decisión:** Backend API implementado como Blueprint en Flask WSGI en vez de FastAPI.
  **Por qué:** PythonAnywhere soporta 1 worker WSGI síncrono; FastAPI no es compatible en este entorno sin workers asíncronos.
- **Decisión:** Hojas de cálculo `Obligaciones_Maestro`, `Presupuesto_Base` y `Soporte_Tickets` serán creadas desde cero.
  **Por qué:** Permite separar responsabilidades financieras e incidencias limpiamente en Google Sheets.
- **Decisión:** Usar `avisos.json` como caché en disco y Google Calendar como fuente de verdad real para los avisos.
  **Por qué:** Mejora la latencia de respuesta hacia la app móvil esquivando llamadas lentas a las APIs de Google por cada request de un worker atareado.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `docs/specs/gamma-mobile/spec.md`, `plan.md`, `tasks.md` | Documentación formal de la arquitectura SDD y la secuencia de tareas obligatoria. |
| `docs/specs/gamma-mobile/backend-spec/*` | Creación de contrato `openapi.yaml`, `REQUISITOS.md` y `mock_server/` para trabajar en frontend sin depender del backend. |
| `HANDOFF-backend.md` | Creado con instrucciones detalladas para el traspaso e implementación de la API. |

## 4. Pendientes explícitos para la próxima fase
- [ ] Configurar o exportar correctamente `JAVA_HOME` localmente para permitir compilaciones de Gradle.
- [ ] Ejecutar M-02 (CI GitHub Actions para Android: assembleDebug + test).
- [ ] Empezar la Fase A (Android Finanzas y Deudas) implementando M-03 (Capa de red, interceptor, timeout).
- [ ] Implementar M-04, M-05 y M-06 estrictamente de forma secuencial y atómica tras M-03.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Limitación estricta de concurrencia y timeout en PythonAnywhere.
  **Impacto:** Tiempos de espera o conexiones rechazadas cuando la app móvil haga peticiones concurrentes o dependientes de Gemini.
  **Mitigación sugerida:** La app móvil (Fase A) debe implementar un interceptor de red muy robusto con timeouts cortos, manejo de 401/500 explícito y comunicación UI clara (loaders e indicadores offline).

## 6. Cómo verificar que este handoff sigue vigente
```bash
# Verifica que se reconozca el proyecto gradle (una vez seteado JAVA_HOME)
./gradlew tasks
# Verifica que el historial git esté limpio y en la rama feat/gamma-mobile
git status
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
