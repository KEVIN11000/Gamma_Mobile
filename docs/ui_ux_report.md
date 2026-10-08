# 📱 Análisis Heurístico de UI/UX (Gamma Mobile)

## 1. Fortalezas (Lo que funciona bien)
- **Gestión de Estados**: Excelente uso de los estados de la UI (`Loading`, `Error`, `Empty`, `Success`), garantizando que el usuario siempre sepa qué hace la aplicación.
- **Componentes Modernos**: Buena adopción de componentes de Material Design 3 (M3) como `Scaffold`, `Cards` y `ExtendedFloatingActionButton`.
- **Feedback Visual**: Uso efectivo de indicadores visuales, como el `LinearProgressIndicator` para el seguimiento de deudas en `ObligacionesScreen` y la codificación de colores para eventos críticos en `DashboardScreen`.
- **Agrupación de Datos**: Agrupación inteligente de transacciones por fecha en `TransaccionesScreen` con cabeceras fijas ("sticky headers"), brindando una sensación natural de libro contable.

## 2. Fricciones y Antipatrones (Problemas de UX/UI)

### A. Fricción de Entrada y Accesibilidad (a11y)
- **Tipos de Teclado**: En `ObligacionesScreen`, los inputs numéricos (Monto, Cuotas) dependen de un filtro de caracteres (`it.all { char.isDigit() }`) pero no especifican `KeyboardOptions(keyboardType = KeyboardType.Number)`. Esto obliga al usuario a escribir números en un teclado alfabético estándar, creando una fricción alta.
- **Lectores de Pantalla**: A algunos iconos importantes les faltan valores contextuales adecuados en `contentDescription` (ej. el icono de alerta crítica en `DashboardScreen` está en `null`).

### B. Problemas de Diseño y Flujo
- **Dashboard No Desplazable**: `DashboardScreen` envuelve su contenido principal en un `Column` estático en lugar de un `LazyColumn` o usar `verticalScroll`. En dispositivos pequeños o modo horizontal, el contenido se recortará, volviendo inaccesibles los elementos inferiores.
- **Acciones Destructivas**: En `AvisosScreen`, la acción de borrado es inmediata al tocar el icono de papelera. No hay diálogo de confirmación ni un `Snackbar` de "Deshacer", lo que puede llevar a pérdida accidental de datos.
- **FABs Apilados**: `ObligacionesScreen` apila dos Botones de Acción Flotante (FABs) verticalmente. Esto es un antipatrón en Material Design; múltiples acciones primarias deberían usar un "Speed Dial" que se expanda, un "Bottom Sheet", o botones estándar.

### C. Estados Vacíos y Navegación
- **Estados Vacíos Pobres**: En todas las pantallas, el estado "Vacío" es solo texto plano centrado. Carece de ilustraciones atractivas o botones de "Llamada a la Acción" (CTA) para guiar al usuario sobre qué hacer a continuación.
- **Componentes Mal Utilizados**: `TransaccionesScreen` usa un `ScrollableTabRow` para filtrar (Todos / Ingresos / Gastos). Las pestañas implican navegar entre vistas/páginas distintas, mientras que para filtrar una lista única se deben usar `FilterChip`s o Botones Segmentados M3.
- **Modales Bruscos**: `AvisosScreen` usa `AlertDialog`s básicos para mostrar detalles simples de un ítem, interrumpiendo el flujo del usuario en lugar de usar una tarjeta expandible (`animateContentSize`) o navegar a una pantalla de detalles.

---

## 3. "Quick Wins" y Mejoras Estructurales Recomendadas

### 🎨 Material Design 3 y Pulido
- **Teclados Numéricos**: Añadir `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)` a todos los `OutlinedTextField` que esperen entrada numérica.
- **Colores Semánticos**: Reemplazar colores hex fijos (ej. `#4CAF50` y `#F44336`) por Colores Semánticos M3 estándar (ej. `MaterialTheme.colorScheme.tertiary` o `error`) para asegurar una compatibilidad nativa con modo oscuro.
- **Chips de Filtro**: Cambiar el `ScrollableTabRow` en `TransaccionesScreen` por un `SingleChoiceSegmentedButtonRow` o un `Row` de `FilterChip`s.
- **Animaciones de Lista**: Agregar `Modifier.animateItem()` a los elementos dentro del `LazyColumn` de `TransaccionesScreen` para que las listas se reordenen y filtren con transiciones suaves.

### 🛠 Flujo de Usuario y Seguridad
- **Borrado Seguro**: Implementar `SwipeToDismissBox` para borrar Avisos, junto con un Snackbar que ofrezca "Deshacer", o como mínimo, un `AlertDialog` de confirmación.
- **Layouts Desplazables**: Envolver el contenido del Dashboard dentro de un `Modifier.verticalScroll(rememberScrollState())` para garantizar que nunca se corte en pantallas pequeñas.
- **Consolidar FABs**: En `ObligacionesScreen`, cambiar los FABs apilados a un único FAB primario ("Acciones") que abra un `ModalBottomSheet` con las opciones "Nueva Obligación" y "Simular Cuota".
- **Estados Vacíos Enriquecidos**: Crear un componente `EmptyState` reutilizable que reciba una ilustración, un mensaje descriptivo y un botón opcional de acción (ej. "Registrar Movimiento"), e implementarlo globalmente.
