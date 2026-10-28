# Quickstart Validation: Flujo de Actualización In-App

## 1. Verificación de Compilación y Suites de Prueba

Ejecutar las pruebas unitarias que validan la lógica de semver y el ViewModel:

```bash
.\gradlew testDebugUnitTest --tests *SemverUtilsTest* --tests *SistemaViewModelTest*
```

Resultado esperado:
- `SemverUtilsTest`: 100% verde (comparaciones `isNewer` válidas con prefijos `v`, hashes de commit, etc.).
- `SistemaViewModelTest`: 100% verde (transiciones a `UpToDate`, `UpdateAvailable` y `CheckFailed`).

## 2. Compilación del Módulo Android

Verificar que la composición de UI en Compose no introduzca errores de tipo o sintaxis:

```bash
.\gradlew assembleDebug
```

## 3. Verificación Visual de Pantalla en Compose

- Ingresar a la sección "Sistema y Soporte" en la aplicación móvil o inspeccionar el composable en preview.
- Confirmar que:
  1. Si la versión remota coincide con la instalada (`v0.0.10`), se visualiza el indicador verde de versión al día.
  2. Si la versión remota fuera superior, aparece la tarjeta con botón "Descargar actualización".
  3. No se producen saltos visuales ni parpadeos durante el refresco.
