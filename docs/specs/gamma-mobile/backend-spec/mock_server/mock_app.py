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

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
