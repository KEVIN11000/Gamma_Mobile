import pytest
import sys
import os

# Añadir mock_server al path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "mock_server")))

from mock_app import app

@pytest.fixture
def client():
    app.config["TESTING"] = True
    with app.test_client() as client:
        yield client

def test_health(client):
    res = client.get("/api/v1/health")
    assert res.status_code == 200
    data = res.get_json()
    assert data["status"] == "ok"
    assert data["sheets"] is True
    assert data["avisos"] is True

def test_dashboard_summary(client):
    res = client.get("/api/v1/dashboard/summary")
    assert res.status_code == 200
    data = res.get_json()
    assert data["mes"] == "Octubre"
    assert "margen_libre" in data
    assert "saldo_acumulado" in data

def test_avisos_endpoints(client):
    # GET avisos
    res = client.get("/api/v1/avisos")
    assert res.status_code == 200
    assert isinstance(res.get_json(), list)

    # POST aviso
    res_post = client.post("/api/v1/avisos", json={"titulo": "Test", "fecha_evento": "14/10/2026 10:00"})
    assert res_post.status_code == 201

    # DELETE aviso
    res_del = client.delete("/api/v1/avisos/av1")
    assert res_del.status_code == 200

def test_asistencia_endpoints(client):
    res_marcar = client.post("/api/v1/asistencia/marcar", json={"modo": "normal"})
    assert res_marcar.status_code == 200
    assert "Marcación laboral exitosa" in res_marcar.get_json()["mensaje"]

    res_materia = client.post("/api/v1/asistencia/materia", json={"materia": "Física"})
    assert res_materia.status_code == 200
    assert "Física" in res_materia.get_json()["mensaje"]

def test_cierre_and_pdf(client):
    res_cierre = client.post("/api/v1/cierres/ejecutar", json={"hoja": "Octubre 2026", "aplicar_descuento": True})
    assert res_cierre.status_code == 200
    data = res_cierre.get_json()
    assert "pdf_url" in data

    res_pdf = client.get("/api/v1/cierres/reportes/cierre_octubre_2026.pdf")
    assert res_pdf.status_code == 200
    assert res_pdf.content_type == "application/pdf"
    assert res_pdf.data.startswith(b"%PDF")

def test_soporte_and_sistema(client):
    res_tickets = client.get("/api/v1/soporte/tickets")
    assert res_tickets.status_code == 200
    assert len(res_tickets.get_json()) >= 2

    res_new_ticket = client.post("/api/v1/soporte/tickets", json={"asunto": "Test Bug", "descripcion": "Detalle"})
    assert res_new_ticket.status_code == 201
    assert res_new_ticket.get_json()["asunto"] == "Test Bug"

    res_logs = client.get("/api/v1/sistema/logs")
    assert res_logs.status_code == 200
    assert len(res_logs.get_json()) >= 1
