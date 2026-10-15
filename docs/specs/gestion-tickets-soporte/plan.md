# Plan de Arquitectura y Diseño Técnico (Plan)
## Módulo: Gestión, Atención y Verificación de Estado de Tickets de Soporte

- **Fecha:** 15 de Octubre de 2026
- **Versión:** 1.0.0
- **Documento de Referencia:** [spec.md](file:///e:/PG/vcp-s/GammaMobile/docs/specs/gestion-tickets-soporte/spec.md)

---

## 1. Contratos de Datos y Esquemas de Validación

### 1.1 Backend Pydantic DTOs (`src/api/blueprint.py` en `Gamma_bot`)

```python
class TicketCreate(BaseModel):
    asunto: str
    descripcion: Optional[str] = None
    origen: Optional[Literal["app", "telegram", "ci", "servidor"]] = "app"

class TicketUpdate(BaseModel):
    estado: Literal["Abierto", "En proceso", "Cerrado"]
    resolucion: Optional[str] = None

class TicketResponse(BaseModel):
    id: str
    asunto: str
    descripcion: str
    estado: str
    fecha: str
    origen: str = "app"
    resolucion: Optional[str] = None
```

- Endpoint: `PATCH /api/v1/soporte/tickets/<ticket_id>`
  - Headers: `X-API-KEY: <key>`
  - Status codes:
    * `200 OK`: Ticket actualizado exitosamente.
    * `400 Bad Request`: Estado inválido o JSON malformado.
    * `404 Not Found`: Ticket inexistente con el ID especificado.
    * `401 Unauthorized`: API Key inválida o ausente.

### 1.2 Android Data Contracts (`GammaMobile`)

```kotlin
// com.gamma.mobile.domain.model.SistemaModels.kt
data class TicketUpdate(
    @SerializedName("estado")
    val estado: String,
    @SerializedName("resolucion")
    val resolucion: String? = null
)

data class TicketItem(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("asunto")
    val asunto: String = "",
    @SerializedName("descripcion")
    val descripcion: String = "",
    @SerializedName("estado")
    val estado: String = "Abierto", // "Abierto", "En proceso", "Cerrado"
    @SerializedName("fecha")
    val fecha: String = "",
    @SerializedName("origen")
    val origen: String = "app", // "app", "telegram", "ci", "servidor"
    @SerializedName("resolucion")
    val resolucion: String? = null
)
```

- Interfaz Retrofit (`GammaApi.kt`):
```kotlin
@PATCH("soporte/tickets/{id}")
suspend fun updateTicket(
    @Path("id") id: String,
    @Body request: TicketUpdate
): Response<TicketItem>
```

---

## 2. Arquitectura Modular y Flujo de Datos

```
+─────────────────────────────────────────────────────────────+
│                       Gamma Mobile UI                       │
│  - SistemaScreen: Filtro de Chips ("Todos", "Abierto", ...) │
│  - Tarjeta con Badge semántico (Verde/Azul/Ámbar)           │
│  - Modal "Atender Ticket" -> Cambiar estado / resolución    │
+──────────────────────────────┬──────────────────────────────+
                               │
                               ▼
+─────────────────────────────────────────────────────────────+
│                     SistemaViewModel                        │
│  - actualizarEstadoTicket(id, estado, resolucion)           │
│  - setFiltroTickets(filtro)                                 │
│  - ticketsFiltrados (StateFlow derivado)                    │
+──────────────────────────────┬──────────────────────────────+
                               │
                               ▼
+─────────────────────────────────────────────────────────────+
│              SistemaRepository / GammaApi                   │
│  - PATCH /api/v1/soporte/tickets/{id}                       │
+──────────────────────────────┬──────────────────────────────+
                               │ HTTP / JSON
                               ▼
+─────────────────────────────────────────────────────────────+
│                 Gamma_bot (Flask Blueprint)                 │
│  - PATCH /api/v1/soporte/tickets/<ticket_id>                │
│  - Actualización atómica en src/data/tickets.json           │
│  - Alerta opcional a Telegram si el estado pasa a "Cerrado" │
+─────────────────────────────────────────────────────────────+
```

---

## 3. Manejo de Errores y Casos Borde

1. **Ticket no encontrado:** El backend devuelve 404 con `{"error": {"code": "NOT_FOUND", "message": "Ticket TCK-xxx no encontrado"}}`. El ViewModel captura el error y despliega el mensaje sin desincronizar la UI.
2. **Error de Red en el Móvil:** Si la petición falla por falta de conectividad, `NetworkResult.Exception` informa al usuario mediante el snackbar/alerta de error estándar sin alterar el estado optimista incorrectamente.
3. **Persistencia Concurrente:** El backend implementa escritura segura de archivo para evitar colisiones si se atiende un ticket mientras otro se reporta.
4. **Aislamiento de Tests en Backend:** En `tests/test_mobile_api.py`, la fixture `client` o un mock dedicado redireccionará `TICKETS_FILE` a un `tmp_path`, evitando la polución continua de `src/data/tickets.json`.

---

## 4. Estrategia de Testing

- **Backend:**
  - Test de `PATCH /api/v1/soporte/tickets/<id>` con transición válida (`Abierto` -> `Cerrado`).
  - Test con estado inválido (debe retornar 400).
  - Test con ID inexistente (debe retornar 404).
  - Verificación de que `tickets.json` no cambia tras la suite completa de tests.
- **Android:**
  - `SistemaViewModelTest`: Prueba unitaria de `actualizarEstadoTicket()` con éxito y fallo.
  - `SistemaViewModelTest`: Prueba de filtrado reactivo de tickets por estado.
