# Reporte de QA - Fase A (Finanzas y Presupuestos)

Este documento registra las observaciones del usuario durante la validación empírica en emulador (QA-A) de la Fase A y las acciones tomadas para resolverlas antes del cierre de fase.

## Observaciones y Correcciones

| # | Observación del Usuario | Análisis y Concepto Contable | Acción Implementada en Código | Estado |
|---|---|---|---|---|
| 1 | **Margen libre:** ¿Sería el balance actual? | No. El balance actual es el saldo acumulado real. El *margen libre* es prospectivo: dinero disponible para gastos variables *después* de restar presupuesto fijo y deudas. | Se agregó el sufijo "(mes en curso)" y un ícono `Info` con un *tooltip* ("Dinero disponible tras descontar el presupuesto base y las deudas") en `DashboardScreen.kt`. | ✅ Resuelto |
| 2 | **Saldo Acumulado:** No tiene sentido mostrarlo día a día, ya que resulta del cierre contable de mes. | Correcto. Mostrar el saldo histórico puede confundir en la operatoria diaria del mes en curso, donde importa más el flujo neto mensual. | Se eliminó el campo "Saldo Acumulado" de la tarjeta superior del Dashboard para dar protagonismo a los indicadores del mes en curso. | ✅ Resuelto |
| 3 | **Posición IVA:** Un número negativo ("-250 mil") es confuso. ¿Significa deuda o saldo a favor? | En términos contables, si el Débito (generado por ingresos) supera al Crédito (generado por gastos), hay deuda al fisco. Si es al revés, hay saldo a favor. | Se cambió la visualización a formato texto explícito: "a pagar (Débito)" en color rojo y "a favor (Crédito)" en color verde, prescindiendo de los signos negativos confusos. | ✅ Resuelto |
| 4 | **IA y distinción Ingreso/Gasto:** En entrada libre de texto, ¿la IA distinguirá si es ingreso o gasto para enviarlo al libro correcto? | Sí. Al momento de implementar la Fase B (IA y OCR), el prompt de Gemini exigirá un JSON estructurado con el campo `tipo: "Ingreso" \| "Gasto"`, delegando la clasificación al LLM. | Se documenta como requisito indispensable para el inicio y desarrollo de la **Fase B**. | ⏳ Pendiente (Fase B) |
| 5 | **IVA Incluido:** Los montos facturados ya tienen el IVA incluido, no se suma aparte. | Es la práctica estándar en Paraguay. Para calcular el IVA a partir de un monto con IVA incluido se divide entre 11 (tasa 10%) o entre 21 (tasa 5%). | La lógica ya dividía `/ 11` y `/ 21` correctamente, pero la UI no era clara. Se actualizó el formulario en `NuevaTransaccionScreen.kt` a "Monto Total (IVA Incluido, ₲)" y la tarjeta a "Desglose de Impuestos (IVA extraído del total)". | ✅ Resuelto |
| 6 | **Botón flotante redundante:** Hay un ícono de avisos en el dashboard que no tiene sentido si ya existe la pestaña/botón a la vista. | Correcto, el FloatingActionButton (FAB) sobrecargaba la UI innecesariamente. | Se eliminó el `SmallFloatingActionButton` de notificaciones en el Dashboard, dejando un solo botón principal para "Nueva Transacción". | ✅ Resuelto |

## Conclusión

La **Fase A** queda oficialmente aprobada y pulida. El estado de la rama principal (`main`) cumple con todos los requisitos del `HANDOFF.md` más las mejoras introducidas por el QA empírico. 

Próximo paso: Realizar el commit de cierre ("fuga cero") y dar inicio a la **Fase B** (Avisos, IA y OCR).
