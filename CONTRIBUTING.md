# 🤝 Guía de Contribución — Gamma Mobile

Gracias por contribuir a **Gamma Mobile**. Para mantener la alta calidad del código, la estabilidad de los releases y la seguridad del ecosistema, sigue estas directrices.

---

## 1. Convenciones de Ramas (Git Flow Simplificado)

* `main`: Rama de producción estable. Contiene código listo para empaquetar en APK de release.
* `feat/<nombre-funcionalidad>`: Nuevas características o pantallas (ej. `feat/asistencia-gps`).
* `fix/<nombre-bug>`: Corrección de errores detectados (ej. `fix/room-sync-crash`).
* `refactor/<descripcion>`: Mejoras de arquitectura o refactorización sin alterar el comportamiento.

---

## 2. Convención de Mensajes de Commit (Conventional Commits)

Los commits deben ser atómicos, descriptivos y seguir el estándar de [Conventional Commits](https://www.conventionalcommits.org/):

```text
<tipo>(<alcance>): <descripción concisa en imperativo>

[cuerpo opcional detallando el motivo o impacto técnico]
```

### Tipos comunes:
* `feat`: Nueva funcionalidad para el usuario.
* `fix`: Corrección de un defecto o bug.
* `docs`: Cambios exclusivamente en documentación (`README.md`, specs, etc.).
* `style`: Ajustes de formato o estilo que no alteran la lógica.
* `refactor`: Refactorización de código de producción.
* `test`: Adición o corrección de pruebas unitarias o de instrumentación.
* `chore`: Mantenimiento de configuración de build, dependencias o tooling.

---

## 3. Política de Fuga Cero y Seguridad

* **Prohibición de `git add .`:** Agrega los archivos modificados de forma individual indicando su ruta explícita (`git add app/...`).
* **Secretos:** Nunca confirmes archivos `local.properties`, certificados `.jks`, `.keystore` o contraseñas en texto plano.
* **Auditoría Pre-Commit:**
  Antes de confirmar cambios, ejecuta:
  ```bash
  git status
  git diff --cached --stat
  ```

---

## 4. Checklist de Verificación Local Antes de Enviar PR

Antes de abrir un Pull Request o solicitar un merge a `main`, asegúrate de que todos los siguientes comandos pasen exitosamente en tu máquina:

```bash
# 1. Compilación de desarrollo
./gradlew assembleDebug

# 2. Pruebas unitarias completas
./gradlew testDebugUnitTest

# 3. Verificación de Lint sin errores críticos
./gradlew lintDebug
```

---

## 5. Proceso de Pull Request

1. Abre el Pull Request contra la rama `main`.
2. Incluye un resumen claro de los cambios implementados y las capturas de pantalla si hay cambios visuales en Compose.
3. Asegúrate de que el pipeline de GitHub Actions (`Android CI`) finalice en verde.
4. Requiere aprobación técnica antes de realizar el merge.
