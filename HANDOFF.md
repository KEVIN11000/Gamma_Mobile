# HANDOFF — Gamma Mobile

**Fase cerrada:** Fase A — Android Finanzas y Deudas
**Fecha de cierre:** 2026-10-08
**Estado del build/tests:** ✅ pasando — Todos los unit tests de los módulos de Finanzas y Deudas pasan con éxito (`./gradlew testDebugUnitTest assembleDebug`).

---

## 1. Estado actual
- [QA-A Manual finalizado satisfactoriamente: feedback contable y de UI aplicado y validado].
- [Dashboard pulido: eliminado FAB redundante, UI de márgenes y Posición IVA corregida].
- [Transacciones (IA/Manual): se añadió la distinción de "Monto Total (IVA Incluido)" visualmente en la pantalla].
- [La arquitectura, Room, UI, Retrofit y WorkManager Offline de la Fase A están totalmente implementados, probados y commiteados en el repositorio base sin fugas (Cero Secretos)].

## 2. Decisiones tomadas en esta fase
- **Decisión:** Ocultar el "Saldo Acumulado" en el Dashboard y usar textos descriptivos para la "Posición IVA" (Crédito/Débito) en vez de números negativos.
  **Por qué:** Para evitar confusión, el usuario necesita indicadores limpios para el flujo neto del mes en curso; el acumulado total solo es relevante tras el cierre de libros.
- **Decisión:** El registro de montos se hace siempre por el Total y la app o backend se encarga del desglose del IVA internamente (dividiendo por 11 o 21).
  **Por qué:** Es el estándar "IVA Incluido" de Paraguay.
- **Decisión:** El prompt de IA / OCR en la Fase B deberá estructurar el JSON resultante especificando el campo tipo ("Ingreso" / "Gasto").
  **Por qué:** Para independizar al usuario de tener que decirle manualmente a la app si una transcripción de texto libre es ingreso o egreso.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `.../presentation/dashboard/DashboardScreen.kt` | Refactor visual de la Card de posición, Tooltips de info añadidos y FAB de avisos removido. |
| `.../presentation/transacciones/NuevaTransaccionScreen.kt` | Aclaraciones visuales sobre montos con IVA incluido y desglose en la vista previa. |
| `docs/specs/gamma-mobile/tasks.md` | QA-A marcado como completado y se actualizó M-16 (IA infiriendo tipo de movimiento). |
| `docs/history/QA_Feedback_Fase_A.md` | Registro formal de la validación empírica y acciones tomadas. |

## 4. Pendientes explícitos para la próxima fase
- [ ] Iniciar la **Fase B — Avisos, IA y OCR**.
- [ ] Implementar M-14 (Pestaña Avisos: lista, crear, borrar, IA).
- [ ] Implementar M-15 (`AvisosWorker` para disparar alarmas con hitos 30/7/5/3/1 días).
- [ ] Implementar M-16 (Registro manual vía IA interpretando texto, con timeout de 45s).
- [ ] Implementar M-17 (Flujo de foto de comprobante, OCR, confirmar/cancelar).

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Existe un *warning* en la compilación por un parámetro no utilizado (`onNuevoRecordatorio`) en el composable `DashboardScreen` (debido a la remoción del FAB).
  **Impacto:** Bajo. No impide la compilación ni crashea.
  **Mitigación sugerida:** Eliminar ese parámetro sobrante en la navegación general cuando se implemente la arquitectura de la Fase B.

## 6. Cómo verificar que este handoff sigue vigente
```bash
./gradlew testDebugUnitTest assembleDebug
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
