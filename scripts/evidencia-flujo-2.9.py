"""Recoge resultados reales de Newman y consulta la H2 de la misma JVM.
Cliente externo de verificación; el backend sigue siendo exclusivamente Java.
Ejecutar después de la primera carpeta Postman (--phase error) y de la compra (--phase final).
"""
import argparse
import html
import json
import re
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path
from xml.etree import ElementTree


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--phase", choices=["error", "final", "seguridad"], required=True)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--variables", required=True, help="Entorno exportado por Newman; no se publica porque contiene JWT temporales.")
    parser.add_argument("--newman-report", required=True)
    args = parser.parse_args()
    output = Path("docs/evidencias-2.9")
    output.mkdir(parents=True, exist_ok=True)
    environment = json.loads(Path(args.variables).read_text(encoding="utf-8-sig"))
    values = {v["key"]: v.get("value", "") for v in environment["values"]}
    report = json.loads(Path(args.newman_report).read_text(encoding="utf-8-sig"))
    assert not report["run"]["failures"], report["run"]["failures"]
    evidence = {"fechaUtc": datetime.now(timezone.utc).isoformat(), "baseUrl": args.base_url,
                "fase": args.phase, "estadisticasNewman": report["run"]["stats"], "http": [], "sql": []}
    for execution in report["run"]["executions"]:
        response = execution.get("response")
        assert response is not None
        stream = response.get("stream", {})
        raw = bytes(stream.get("data", [])).decode("utf-8") if isinstance(stream, dict) else str(stream)
        try:
            body = json.loads(raw)
        except ValueError:
            body = raw
        if isinstance(body, dict) and "token" in body:
            body["token"] = "[JWT omitido]"
        if isinstance(body, dict) and "openapi" in body:
            body = {"securitySchemes": body["components"]["securitySchemes"],
                    "operaciones": [method.upper() + " " + path for path, item in body["paths"].items()
                                    for method in item if method in ["get", "post", "patch", "delete"]]}
        request = execution["request"]
        request_body = request.get("body", {}).get("raw")
        if request_body:
            try:
                request_body = json.loads(request_body)
                if "password" in request_body:
                    request_body["password"] = "[contraseña ficticia omitida]"
            except ValueError:
                pass
        evidence["http"].append({"paso": execution["item"]["name"], "metodo": request["method"],
            "url": request["url"], "request": request_body, "status": response["code"], "response": body,
            "aserciones": [{"nombre": a["assertion"], "correcta": not a.get("error")} for a in execution.get("assertions", [])]})
    evidence["muestras"] = {k: values[k] for k in ["vendedorId", "compradorId", "prendaId", "varianteId", "bolsaId", "pedidoId"] if values.get(k)}
    if args.phase != "seguridad":
        # IFEXISTS evita crear otra base por un nombre equivocado; la consola vive en la JVM de Spring.
        h2 = args.base_url.rstrip("/") + "/h2-console/"
        welcome = urllib.request.urlopen(h2, timeout=30).read().decode()
        session = re.search(r"jsessionid=([a-z0-9]+)", welcome)[1]
        form = urllib.parse.urlencode({"driver": "org.h2.Driver", "url": "jdbc:h2:mem:ecommercedb;IFEXISTS=TRUE",
                                      "user": "sa", "password": ""}).encode()
        login = urllib.request.urlopen(h2 + "login.do?jsessionid=" + session, form, timeout=30).read().decode()
        assert 'name="h2query"' in login

        def sql_query(name, sql):
            form = urllib.parse.urlencode({"sql": sql, "maxrows": "100"}).encode()
            result = urllib.request.urlopen(h2 + "query.do?jsessionid=" + session, form, timeout=30).read().decode()
            match = re.search(r'<table class="resultSet".*?</table>', result, re.S)
            assert match, result
            table = ElementTree.fromstring(html.unescape(match[0]))
            headers = [cell.text for cell in table[0]]
            rows = [dict(zip(headers, [cell.text for cell in row])) for row in list(table)[1:]]
            (output / (args.phase + "-" + name + ".html")).write_text(result, encoding="utf-8")
            evidence["sql"].append({"consulta": sql, "filas": rows})
            return rows

        bolsa, variante = str(uuid.UUID(values["bolsaId"])), str(uuid.UUID(values["varianteId"]))
        count = sql_query("pedidos-por-bolsa", "SELECT COUNT(*) AS cantidad FROM pedidos_moda WHERE bolsa_id='" + bolsa + "'")
        stock = sql_query("inventario", "SELECT stock_disponible FROM variantes_prenda WHERE id='" + variante + "'")
        estado = sql_query("bolsa", "SELECT cerrada FROM bolsas_estilo WHERE id='" + bolsa + "'")
        if args.phase == "error":
            lineas = sql_query("lineas-bolsa", "SELECT COUNT(*) AS cantidad FROM lineas_bolsa WHERE eliminado=FALSE AND bolsa_id='" + bolsa + "'")
            assert count[0]["CANTIDAD"] == "0" and stock[0]["STOCK_DISPONIBLE"] == "3"
            assert estado[0]["CERRADA"] == "FALSE" and lineas[0]["CANTIDAD"] == "0"
        else:
            pedido = str(uuid.UUID(values["pedidoId"]))
            rows = sql_query("pedido-historico", """SELECT p.id,p.bolsa_id,p.cliente_id,p.estado,p.fecha_creacion,p.total,p.moneda,
                l.prenda_id,l.variante_id,l.sku,l.tipo_talla,l.cantidad,l.precio_unitario,
                l.cantidad*l.precio_unitario AS subtotal FROM pedidos_moda p
                JOIN lineas_pedido l ON l.pedido_id=p.id WHERE p.id='""" + pedido + "' ORDER BY l.posicion")
            assert count[0]["CANTIDAD"] == "1" and stock[0]["STOCK_DISPONIBLE"] == "1"
            assert estado[0]["CERRADA"] == "TRUE" and len(rows) == 1
            assert rows[0]["ESTADO"] == "PENDIENTE" and rows[0]["CLIENTE_ID"] == values["compradorId"]
            assert rows[0]["CANTIDAD"] == "2" and Decimal(rows[0]["PRECIO_UNITARIO"]) == Decimal("19.95")
            assert Decimal(rows[0]["TOTAL"]) == Decimal("39.90") and Decimal(rows[0]["SUBTOTAL"]) == Decimal("39.90")
        urllib.request.urlopen(h2 + "logout.do?jsessionid=" + session, timeout=30).close()
    evidence["resultado"] = "OK: aserciones Postman y comprobaciones SQL satisfactorias."
    (output / (args.phase + ".json")).write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"fase": args.phase, "http": len(evidence["http"]), "sql": len(evidence["sql"]), "resultado": "OK"}))


if __name__ == "__main__":
    main()
