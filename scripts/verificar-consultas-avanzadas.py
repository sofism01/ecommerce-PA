"""Verificación HTTP y SQL real. Ejecutar contra una instancia recién iniciada con los datos demo.

python scripts/verificar-consultas-avanzadas.py --base-url http://localhost:8081
Usa sólo la biblioteca estándar. Crea una prenda y una bolsa demo para verificar las operaciones previas.
"""
import argparse
import html
import json
import re
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timezone
from pathlib import Path
from xml.etree import ElementTree


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://localhost:8081")
    parser.add_argument("--output", default="docs/evidencias-2.6")
    args = parser.parse_args()
    base = args.base_url.rstrip("/")
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    evidence = {"fechaUtc": datetime.now(timezone.utc).isoformat(), "baseUrl": base, "http": [], "sql": []}

    def request(path, expected=200, method="GET", body=None):
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(base + path, data=data, method=method,
                                     headers={"Content-Type": "application/json"} if data else {})
        try:
            response = urllib.request.urlopen(req, timeout=30)
        except urllib.error.HTTPError as error:
            response = error
        raw = response.read().decode("utf-8")
        assert response.code == expected, (method, path, response.code, raw)
        content = json.loads(raw) if raw and "application/json" in response.headers.get("Content-Type", "") else None
        evidence["http"].append({"method": method, "path": path, "status": response.code, "body": content})
        return content, raw

    _, ui = request("/swagger-ui/index.html")
    assert "swagger-ui" in ui
    _, initializer = request("/swagger-ui/swagger-initializer.js")
    assert "swagger-config" in initializer
    config, _ = request("/api-docs/swagger-config")
    assert config["url"] == "/api-docs"
    api, _ = request("/api-docs")
    operations = sorted(f"{method.upper()} {path}" for path, item in api["paths"].items()
                        for method in item if method in {"get", "post", "patch", "delete", "put"})
    assert len(operations) == 10, operations
    evidence["operacionesOpenApi"] = operations
    # El esquema completo no se duplica en el informe HTTP.
    evidence["http"][-1]["body"] = {"operaciones": operations}
    for path in ("/api/prendas", "/api/reportes/creadores/variedad"):
        media = api["paths"][path]["get"]["responses"]["200"]["content"]["application/json"]
        assert media.get("examples") or media.get("example")
    assert {p["name"] for p in api["paths"]["/api/prendas"]["get"]["parameters"]} == {"q", "creadorId", "moneda", "page", "size"}

    a = "81000000-0000-4000-8000-000000000001"
    b = "81000000-0000-4000-8000-000000000002"
    todas, _ = request("/api/prendas")
    assert todas["totalElements"] == 3 and todas["totalPages"] == 1
    assert [p["nombre"] for p in todas["content"]] == ["Camisa de lino artesanal", "Chaqueta de autor", "Vestido de algodón"]
    for q in ("LINO", "artesanal", "cAmIsA", "confeccionada"):
        pagina, _ = request("/api/prendas?" + urllib.parse.urlencode({"q": q}))
        assert pagina["totalElements"] == 1 and pagina["content"][0]["nombre"] == "Camisa de lino artesanal"
    pagina, _ = request("/api/prendas?creadorId=" + a)
    assert pagina["totalElements"] == 2
    pagina, _ = request("/api/prendas?moneda=usd")
    assert pagina["totalElements"] == 1 and pagina["content"][0]["creadorId"] == b
    pagina, _ = request("/api/prendas?q=LINO&moneda=COP&creadorId=" + a)
    assert pagina["totalElements"] == 1
    for page, count in ((0, 2), (1, 1), (2, 0)):
        pagina, _ = request(f"/api/prendas?page={page}&size=2")
        assert len(pagina["content"]) == count and pagina["totalElements"] == 3 and pagina["totalPages"] == 2
    vacia, _ = request("/api/prendas?q=sin-coincidencias")
    assert vacia == {"content": [], "totalElements": 0, "totalPages": 0, "page": 0, "size": 20}
    for query in ("page=-1", "size=0", "size=101", "page=abc", "creadorId=invalido", "moneda=123"):
        error, _ = request("/api/prendas?" + query, expected=400)
        assert error["code"] == "DATOS_INVALIDOS"
    ranking, _ = request("/api/reportes/creadores/variedad")
    assert ranking == [{"creadorId": a, "cantidadVariantes": 4, "cantidadPrendas": 2},
                       {"creadorId": b, "cantidadVariantes": 1, "cantidadPrendas": 1}]

    # La consola conecta desde el proceso de Spring a SU H2; no crea otra base externa.
    h2 = base + "/h2-console/"
    welcome = urllib.request.urlopen(h2, timeout=30).read().decode()
    session = re.search(r"jsessionid=([a-z0-9]+)", welcome)[1]
    form = urllib.parse.urlencode({"driver": "org.h2.Driver", "url": "jdbc:h2:mem:ecommercedb;IFEXISTS=TRUE",
                                  "user": "sa", "password": ""}).encode()
    login = urllib.request.urlopen(h2 + "login.do?jsessionid=" + session, form, timeout=30).read().decode()
    assert 'name="h2query"' in login, login

    def sql_query(name, sql):
        form = urllib.parse.urlencode({"sql": sql, "maxrows": "100"}).encode()
        result = urllib.request.urlopen(h2 + "query.do?jsessionid=" + session, form, timeout=30).read().decode()
        match = re.search(r'<table class="resultSet".*?</table>', result, re.S)
        assert match, result
        table = ElementTree.fromstring(html.unescape(match[0]))
        headers = [cell.text for cell in table[0]]
        rows = [dict(zip(headers, [cell.text for cell in row])) for row in list(table)[1:]]
        (output / (name + ".html")).write_text(result, encoding="utf-8")
        evidence["sql"].append({"nombre": name, "consulta": sql, "filas": rows})
        return rows

    rows = sql_query("catalogo-h2", """SELECT p.id, p.creador_id, p.nombre, p.moneda,
        p.disponible_para_venta, p.eliminado, COUNT(v.id) AS variantes
        FROM prendas_autor p JOIN variantes_prenda v ON v.prenda_id=p.id
        WHERE p.eliminado=FALSE AND v.eliminado=FALSE
        GROUP BY p.id, p.creador_id, p.nombre, p.moneda, p.disponible_para_venta, p.eliminado
        ORDER BY p.nombre, p.id""")
    assert len(rows) == todas["totalElements"]
    rows = sql_query("ranking-h2", """SELECT p.creador_id, COUNT(v.id) AS cantidad_variantes,
        COUNT(DISTINCT p.id) AS cantidad_prendas FROM prendas_autor p
        JOIN variantes_prenda v ON v.prenda_id=p.id
        WHERE p.eliminado=FALSE AND v.eliminado=FALSE AND p.disponible_para_venta=TRUE
        GROUP BY p.creador_id ORDER BY COUNT(v.id) DESC, p.creador_id ASC""")
    assert [{"creadorId": r["CREADOR_ID"], "cantidadVariantes": int(r["CANTIDAD_VARIANTES"]),
             "cantidadPrendas": int(r["CANTIDAD_PRENDAS"])} for r in rows] == ranking
    rows = sql_query("pagina-h2", """SELECT p.id, p.nombre FROM prendas_autor p
        WHERE p.eliminado=FALSE AND p.moneda='COP' AND
        (LOWER(p.nombre) LIKE '%lino%' OR LOWER(p.descripcion) LIKE '%lino%')
        ORDER BY p.nombre, p.id OFFSET 0 ROWS FETCH FIRST 1 ROWS ONLY""")
    assert len(rows) == 1 and rows[0]["NOMBRE"] == "Camisa de lino artesanal"
    urllib.request.urlopen(h2 + "logout.do?jsessionid=" + session, timeout=30).close()

    # Ocho operaciones anteriores. La muestra creada queda suspendida y la bolsa cerrada.
    creador = str(uuid.uuid4())
    prenda, _ = request("/api/prendas", expected=201, method="POST", body={
        "creadorId": creador, "nombre": "Prenda verificación 2.6", "descripcion": "Muestra HTTP",
        "precioBase": 50, "moneda": "COP", "variantes": [{"sku": "VER-26-M", "tipoTalla": "M", "stockDisponible": 3}], "stockTotal": 3})
    pid, vid = prenda["prendaId"], prenda["variantes"][0]["varianteId"]
    anteriores, _ = request("/api/prendas/creador/" + creador)
    assert anteriores[0]["prendaId"] == pid
    request(f"/api/prendas/{pid}/variantes/{vid}/stock", expected=204, method="PATCH", body={"nuevoStock": 2})
    request(f"/api/prendas/{pid}/suspender", expected=204, method="PATCH")
    anteriores, _ = request("/api/prendas/creador/" + creador)
    assert not anteriores[0]["disponibleParaLaVenta"] and anteriores[0]["variantes"][0]["stockDisponible"] == 2
    cliente = str(uuid.uuid4())
    bolsa, _ = request("/api/bolsas", expected=201, method="POST", body={"clienteId": cliente})
    activa, _ = request("/api/bolsas/activas/" + cliente)
    bid = activa["bolsaId"]
    assert bolsa["bolsaId"] == bid
    request(f"/api/bolsas/{bid}/items", expected=204, method="DELETE")
    request(f"/api/bolsas/{bid}/cerrar", expected=204, method="PATCH")
    request("/api/bolsas/activas/" + cliente, expected=404)
    ranking_final, _ = request("/api/reportes/creadores/variedad")
    assert ranking_final == ranking

    evidence["resultado"] = "Todas las aserciones HTTP, OpenAPI y SQL fueron satisfactorias."
    evidence["mutacionesDemo"] = {"prendaSuspendida": pid, "bolsaCerrada": bid}
    (output / "verificacion.json").write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"resultado": "OK", "peticionesHttp": len(evidence["http"]),
                      "operacionesOpenApi": len(operations), "consultasSql": len(evidence["sql"]),
                      "evidencia": str(output / "verificacion.json")}, ensure_ascii=False))


if __name__ == "__main__":
    main()
