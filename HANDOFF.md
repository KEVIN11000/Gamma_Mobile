# HANDOFF — Gamma Mobile

**Fase cerrada:** Refinamiento Operativo del Dashboard y Sistema (v0.0.4)
**Fecha de cierre:** 2026-10-14
**Estado del build/tests:** ✅ pasando — Compilación y suites de tests unitarios verificados exitosamente (`./gradlew testDebugUnitTest assembleDebug` en GammaMobile y `pytest tests/test_mobile_api.py` en Gamma_bot).

---

## 1. Estado actual

- **Atajo de Marcación Laboral en Dashboard:** Tarjeta destacada "Marcación Laboral de Hoy" visible de Lunes a Viernes para registro rápido de entrada/salida, cubierta por tests en `DashboardViewModelTest`.
- **Orden Cronológico Real en Libro Diario:** Parser multi-formato (`dd/MM/yyyy`, `d/M/yyyy`, `yyyy-MM-dd`, `dd/MM`) en `TransaccionesViewModel` que corrige la distorsión lexicográfica de agosto y prioriza fechas recientes, cubierto por tests en `TransaccionesViewModelTest`.
- **Métricas de Margen Libre e IVA:** Tarjetas en Guaraníes (`₲`) con badges de Crédito/Débito fiscal en Dashboard, y cálculo en backend (`financiero.py` y `blueprint.py`) cubierto por tests en `test_mobile_api.py`.
- **Filtros de Deudas y ExtendedFAB:** Barra de chips (`Todas`, `Activas`, `Saldadas`, `Simuladas`) y botón flotante extendido en Compromisos, cubierto por tests en `ObligacionesViewModelTest`.
- **Materias Académicas Oficiales:** Conexión dinámica desde la hoja `Config_bot` en Google Sheets vía `GET /asistencia/materias`, eliminando mock data, cubierto por tests en `AsistenciaViewModelTest`.
- **Cierre Dual:** Soporte de cierre Laboral (horas) y Contable (Libro Diario) en backend y UI móvil, cubierto por tests en `CierreViewModelTest`.
- **Google Calendar en Avisos:** Combinación reactiva de eventos de Google Calendar en `GET /avisos` vía `AgenteAutonomoHoras.obtener_avisos_calendar()`.
- **Seguridad y Ergonomía en Sistema:** Candado de solo lectura por defecto en URL/API Key con desbloqueo explícito, y acordeón colapsable con badge de conteo para tickets de soporte en `SistemaScreen.kt`.
- **Release y Tags:** Rama `main` en `Gamma_Mobile` sincronizada, APK compilado en `app/build/outputs/apk/debug/app-debug.apk` y tag `v0.0.4` publicado en GitHub.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Comparador cronológico con parsing multi-formato en `TransaccionesViewModel`.
  **Por qué:** El ordenamiento de strings `sortedByDescending { it.fecha }` ordenaba alfabéticamente ("31/08" antes que "14/10").
  **Alternativas descartadas:** Delegar el ordenamiento al backend de Sheets (inestable ante ediciones manuales).
- **Decisión:** Candado de edición en `SistemaScreen`.
  **Por qué:** Proteger los campos críticos de conexión contra modificaciones táctiles accidentales.
  **Alternativas descartadas:** Ocultar los campos en otra pantalla.
- **Decisión:** Acordeón colapsable para tickets de soporte.
  **Por qué:** Evitar sobrecargar el scroll vertical cuando existen múltiples tickets acumulados.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardScreen.kt` | Atajo de marcación laboral de lunes a viernes y tarjetas de Margen Libre e IVA en Guaraníes. |
| `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardViewModel.kt` | Inyección de `AsistenciaRepository` para marcación directa desde el inicio. |
| `app/src/main/java/com/gamma/mobile/presentation/transacciones/TransaccionesViewModel.kt` | Función `parseFecha` y orden cronológico real descendente. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesScreen.kt` | Fila de chips de filtro (`Todas`, `Activas`, `Saldadas`, `Simuladas`) y ExtendedFAB. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModel.kt` | Estado de filtrado reactivo de deudas. |
| `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaViewModel.kt` | Carga de materias oficiales desde el repositorio. |
| `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaScreen.kt` | Selector de cierre dual (Laboral vs Contable). |
| `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt` | Candado de configuración y acordeón para tickets de soporte. |
| `Gamma_bot (src/logic/financiero.py)` | Cálculo de posición neta de IVA del período no cerrado. |
| `Gamma_bot (src/api/blueprint.py)` | Endpoint `GET /asistencia/materias`, combinación de Calendar en `/avisos` y cierre dual en `/cierres/ejecutar`. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Implementar pipeline en `Gamma_bot` que al detectar un ingreso en `Libro_Diario` con categoría/motivo "Salario" (o "Sueldo"), actualice o inserte automáticamente la fila salarial en la hoja `Presupuesto_Base` (`Monto_Mensual_Gs`).
- [ ] Implementar fallback dinámico en `calcular_margen_de_ataque()` para leer el último salario percibido desde `Libro_Diario` si `Presupuesto_Base` no tiene ingreso registrado (`Margen = Salario - Gastos Fijos - Cuotas Activas`).
- [ ] Corregir el comando de cierre contable en el core de `Gamma_bot` para permitir el corte mensual de ingresos/egresos del Libro Diario.
- [ ] Ejecutar despliegue en servidor PythonAnywhere (`cd ~/Gamma_bot && git pull origin Main-stable` y recarga del worker WSGI).
- [ ] Instalar el APK `v0.0.4` generado en el teléfono físico y verificar visualización de las nuevas tarjetas y filtros.
- [ ] Implementar sincronización bidireccional del estado de los tickets de soporte con GitHub Issues (Abierto / Cerrado).

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Desincronización del backend en PythonAnywhere respecto al repositorio GitHub.
  **Impacto:** Hasta que no se ejecute el pull y reload en PythonAnywhere, los endpoints responderán con la versión anterior (margen libre en 0 y 404 en materias oficiales).
  **Mitigación sugerida:** Realizar `git pull origin Main-stable` y recargar la Web App en la consola de PythonAnywhere.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# En GammaMobile:
./gradlew testDebugUnitTest assembleDebug

# En Gamma_bot:
pytest tests/test_mobile_api.py
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
