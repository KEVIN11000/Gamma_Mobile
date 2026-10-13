import os
from flask import Flask, jsonify, request

app = Flask(__name__)

# Mock validación X-API-KEY
API_KEY = "dev-key"

@app.before_request
def check_api_key():
    pass # Bypassing API key for debug

@app.route("/api/v1/health")
def health():
    return jsonify({"status": "ok", "sheets": True, "avisos": True, "version_api": "1.0.0"})

@app.route("/api/v1/dashboard/summary")
def summary():
    return jsonify({
        "mes": "Octubre",
        "ingresos": 5000000,
        "gastos": 1500000,
        "flujo_neto": 3500000,
        "margen_libre": 2000000,
        "saldo_acumulado": 15000000,
        "posicion_iva": -250000,
        "es_debito_fiscal": False
    })

@app.route("/api/v1/avisos")
def get_avisos():
    return jsonify([
        {"id": "av1", "titulo": "Pagar tarjeta", "fecha_evento": "10/10/2026 10:00", "dias_restantes": 5}
    ])

@app.route("/api/v1/movimientos", methods=["GET"])
def get_movimientos():
    return jsonify([])

@app.route("/api/v1/movimientos", methods=["POST"])
def create_movimiento():
    data = request.json
    return jsonify(data)

import time

@app.route("/api/v1/ia/interpretar", methods=["POST"])
def interpretar_ia():
    data = request.json
    texto = data.get("texto", "").lower()
    time.sleep(2) # Simular latencia de IA
    if "compr" in texto or "gast" in texto or "pagu" in texto:
        return jsonify({"tipo": "Gasto", "categoria": "Simulada IA", "total": 150000, "fecha": "2026-10-08"})
    return jsonify({"tipo": "Ingreso", "categoria": "Simulada IA", "total": 300000, "fecha": "2026-10-08"})

@app.route("/api/v1/movimientos/ocr", methods=["POST"])
def interpretar_ocr():
    # request.files.get("file")
    time.sleep(2) # Simular latencia de OCR
    return jsonify({"tipo": "Gasto", "categoria": "Factura OCR", "total": 85000, "fecha": "2026-10-08"})

@app.route("/api/v1/ia/interpretar/aviso", methods=["POST"])
def interpretar_aviso():
    time.sleep(1)
    return jsonify({"id": "ia-av1", "titulo": "Aviso sugerido por IA", "fecha_evento": "12/10/2026 15:00", "dias_restantes": 4})

@app.route("/api/v1/avisos", methods=["POST"])
def create_aviso():
    return jsonify({"status": "created"}), 201

@app.route("/api/v1/avisos/<id>", methods=["DELETE"])
def delete_aviso(id):
    return jsonify({"status": "deleted"}), 200

@app.route("/api/v1/asistencia/marcar", methods=["POST"])
def marcar_asistencia():
    data = request.json or {}
    modo = data.get("modo", "normal")
    return jsonify({
        "tipo": "entrada",
        "hora": "08:15",
        "mensaje": f"Marcación laboral exitosa en modo {modo}"
    })

@app.route("/api/v1/asistencia/materia", methods=["POST"])
def marcar_materia():
    data = request.json or {}
    materia = data.get("materia", "General")
    return jsonify({
        "materia": materia,
        "hora": "18:30",
        "mensaje": f"Asistencia a materia '{materia}' registrada con éxito"
    })

@app.route("/api/v1/cierres/ejecutar", methods=["POST"])
def ejecutar_cierre():
    data = request.json or {}
    hoja = data.get("hoja", "Octubre 2026")
    con_desc = data.get("aplicar_descuento", False)
    desc_str = " (con descuento aplicado)" if con_desc else ""
    return jsonify({
        "resumen": f"Cierre mensual completado para período {hoja}{desc_str}.",
        "pdf_url": "http://127.0.0.1:5000/api/v1/cierres/reportes/cierre_octubre_2026.pdf",
        "total_horas": 160.0,
        "total_monto": 4500000
    })

@app.route("/api/v1/cierres/reportes/<path:filename>", methods=["GET"])
def descargar_reporte_pdf(filename):
    # Generar un PDF simulado básico en memoria
    dummy_pdf = b"%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF"
    return dummy_pdf, 200, {"Content-Type": "application/pdf"}

MOCK_TICKETS = [
    {"id": "TCK-001", "asunto": "Error al sincronizar movimiento", "descripcion": "Falló en offline temporalmente", "estado": "Cerrado", "fecha": "08/10/2026"},
    {"id": "TCK-002", "asunto": "Consulta de margen libre", "descripcion": "Revisar cálculo con deudas", "estado": "Abierto", "fecha": "11/10/2026"}
]

@app.route("/api/v1/soporte/tickets", methods=["GET"])
def get_tickets():
    return jsonify(MOCK_TICKETS)

@app.route("/api/v1/soporte/tickets", methods=["POST"])
def create_ticket():
    data = request.json or {}
    new_ticket = {
        "id": f"TCK-{len(MOCK_TICKETS) + 1:03d}",
        "asunto": data.get("asunto", "Sin asunto"),
        "descripcion": data.get("descripcion", ""),
        "estado": "Abierto",
        "fecha": "12/10/2026"
    }
    MOCK_TICKETS.insert(0, new_ticket)
    return jsonify(new_ticket), 201

@app.route("/api/v1/sistema/logs", methods=["GET"])
def get_logs():
    return jsonify([
        {"timestamp": "12/10/2026 12:00:00", "nivel": "INFO", "mensaje": "Worker de avisos ejecutado con éxito."},
        {"timestamp": "12/10/2026 13:15:22", "nivel": "INFO", "mensaje": "Cierre mensual ejecutado para Octubre 2026."},
        {"timestamp": "12/10/2026 15:45:10", "nivel": "WARN", "mensaje": "Respuesta lenta simulada en OCR (2000 ms)."}
    ])


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)

