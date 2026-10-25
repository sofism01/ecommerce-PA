"""Verificación HTTP real de 2.7 y SQL sobre la H2 del perfil local dev.
python scripts/verificar-seguridad-jwt.py --base-url http://localhost:PUERTO
Los usuarios/muestras generados son ficticios. No registra JWT ni contraseñas en las evidencias.
"""
import argparse
import base64
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
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--output", default="docs/evidencias-2.7")
    args = parser.parse_args()
    base = args.base_url.rstrip("/")
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    evidence = {"fechaUtc": datetime.now(timezone.utc).isoformat(), "baseUrl": base, "http": [], "sql": []}

    def request(path, expected=200, method="GET", body=None, token=None):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        req = urllib.request.Request(base + path, method=method, headers=headers,
                                     data=json.dumps(body).encode() if body is not None else None)
        try:
            response = urllib.request.urlopen(req, timeout=30)
        except urllib.error.HTTPError as error:
            response = error
        raw = response.read().decode("utf-8")
        assert response.code == expected, (method, path, response.code, raw)
        content = json.loads(raw) if raw and "application/json" in response.headers.get("Content-Type", "") else None
        stored = dict(content) if isinstance(content, dict) else content
        if isinstance(stored, dict) and "token" in stored:
            stored["token"] = "[JWT omitido; claims guardados por separado]"
        assert "passwordHash" not in raw and "password_hash" not in raw
        if expected in (401, 403):
            assert content["status"] == expected and isinstance(content["details"], dict)
            assert content["code"] == ("NO_AUTENTICADO" if expected == 401 else "ACCESO_DENEGADO")
        assert response.headers.get("Set-Cookie") is None, "La API debe ser stateless"
        evidence["http"].append({"method": method, "path": path, "status": response.code, "body": stored})
        return content, raw

    password = "PruebaHttp2026!"
    suffix = uuid.uuid4().hex[:10]

    def account(label, role):
        email = label + "-" + suffix + "@boheme.test"
        user, _ = request("/api/auth/register", 201, "POST", {"nombre": "Cuenta HTTP " + label,
                          "correo": email, "password": password, "rol": role})
        auth, _ = request("/api/auth/login", 200, "POST", {"correo": email, "password": password})
        assert auth["tokenType"] == "Bearer" and auth["rol"] == role and auth["expiresIn"] == 3600
        claims = json.loads(base64.urlsafe_b64decode(auth["token"].split(".")[1] + "=="))
        assert claims["sub"] == user["id"] and claims["rol"] == role and claims["correo"] == email
        assert claims["exp"] - claims["iat"] == 3600
        assert datetime.fromisoformat(auth["expiresAt"]).timestamp() == claims["exp"]
        evidence.setdefault("claims", []).append(claims)
        return user, auth["token"]

    buyer, bt = account("comprador", "COMPRADOR")
    seller, st = account("vendedor", "VENDEDOR")
    other_buyer, obt = account("otro-comprador", "COMPRADOR")
    other_seller, ost = account("otro-vendedor", "VENDEDOR")
    request("/api/auth/register", 409, "POST", {"nombre": "Duplicado", "correo": buyer["correo"].upper(),
            "password": password, "rol": "COMPRADOR"})
    request("/api/auth/register", 400, "POST", {"nombre": "Inválido", "correo": "invalido", "password": "corta", "rol": "COMPRADOR"})
    request("/api/auth/register", 400, "POST", {"nombre": "Inválido", "correo": "admin@boheme.test", "password": password, "rol": "ADMIN"})
    for correo in (buyer["correo"], "no-existe@boheme.test"):
        request("/api/auth/login", 401, "POST", {"correo": correo, "password": "Incorrecta2026!"})

    prenda_body = {"creadorId": seller["id"], "nombre": "Camisa verificación JWT", "descripcion": "Muestra local 2.7",
                  "precioBase": 40, "moneda": "COP", "stockTotal": 3,
                  "variantes": [{"sku": "HTTP-JWT-M", "tipoTalla": "M", "stockDisponible": 3}]}
    request("/api/prendas", 401, "POST", prenda_body)
    request("/api/prendas", 403, "POST", prenda_body, bt)
    request("/api/prendas", 403, "POST", {**prenda_body, "creadorId": other_seller["id"]}, st)
    prenda, _ = request("/api/prendas", 201, "POST", prenda_body, st)
    pid, vid = prenda["prendaId"], prenda["variantes"][0]["varianteId"]
    request(f"/api/prendas/{pid}/suspender", 403, "PATCH", token=ost)
    request(f"/api/prendas/{pid}/variantes/{vid}/stock", 403, "PATCH", {"nuevoStock": 0}, ost)
    request(f"/api/prendas/{pid}/variantes/{vid}/stock", 204, "PATCH", {"nuevoStock": 2}, st)
    request(f"/api/prendas/{pid}/suspender", 204, "PATCH", token=st)
    request(f"/api/prendas/{uuid.uuid4()}/suspender", 404, "PATCH", token=st)

    bolsa_body = {"clienteId": buyer["id"]}
    request("/api/bolsas", 403, "POST", bolsa_body, st)
    request("/api/bolsas", 403, "POST", {"clienteId": other_buyer["id"]}, bt)
    bolsa, _ = request("/api/bolsas", 201, "POST", bolsa_body, bt)
    bid = bolsa["bolsaId"]
    request("/api/bolsas/activas/" + buyer["id"], 403, token=obt)
    request(f"/api/bolsas/{bid}/items", 403, "DELETE", token=obt)
    request(f"/api/bolsas/{bid}/cerrar", 403, "PATCH", token=obt)
    request("/api/bolsas/activas/" + buyer["id"], token=bt)
    request(f"/api/bolsas/{bid}/items", 204, "DELETE", token=bt)
    request(f"/api/bolsas/{bid}/cerrar", 204, "PATCH", token=bt)
    request("/api/bolsas/activas/" + buyer["id"], 404, token=bt)
    # Una firma alterada se rechaza sin llegar al controlador.
    header, payload, signature = st.split(".")
    broken = header + "." + payload + "." + ("A" if signature[0] != "A" else "B") + signature[1:]
    request("/api/prendas", 401, "POST", prenda_body, broken)
    for path in ("/api/prendas", "/api/prendas?q=LINO&moneda=COP&page=0&size=1",
                 "/api/prendas/creador/" + seller["id"], "/api/reportes/creadores/variedad"):
        request(path)

    api, _ = request("/api-docs")
    assert api["components"]["securitySchemes"]["bearerAuth"]["scheme"] == "bearer"
    operations = []
    for path, item in api["paths"].items():
        for method, operation in item.items():
            operations.append(method.upper() + " " + path)
            protected = path.startswith("/api/bolsas") or path.startswith("/api/prendas") and method != "get"
            assert bool(operation.get("security")) == protected
            if protected:
                assert "401" in operation["responses"] and "403" in operation["responses"]
    assert len(operations) == 12
    evidence["http"][-1]["body"] = {"operaciones": sorted(operations), "bearerAuth": api["components"]["securitySchemes"]["bearerAuth"]}
    _, ui = request("/swagger-ui/index.html")
    assert "swagger-ui" in ui

    # Las cuentas demo se asocian a los UUID de los datos iniciales.
    for email in ("vendedor1@boheme.test", "vendedor2@boheme.test", "comprador1@boheme.test", "comprador2@boheme.test"):
        auth, _ = request("/api/auth/login", 200, "POST", {"correo": email, "password": "BohemeDemo2026!"})
        assert auth["usuario"]["id"].startswith("81000000" if email.startswith("vendedor") else "c1000000")

    h2 = base + "/h2-console/"
    welcome = urllib.request.urlopen(h2, timeout=30).read().decode()
    session = re.search(r"jsessionid=([a-z0-9]+)", welcome)[1]
    form = urllib.parse.urlencode({"driver": "org.h2.Driver", "url": "jdbc:h2:mem:ecommercedb;IFEXISTS=TRUE", "user": "sa", "password": ""}).encode()
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
        (output / (name + ".html")).write_text(result, encoding="utf-8")
        evidence["sql"].append({"nombre": name, "consulta": sql, "filas": rows})
        return rows

    rows = sql_query("usuarios-h2", "SELECT id, correo, rol, activo, LEFT(password_hash,7) AS bcrypt_prefijo, LENGTH(password_hash) AS hash_longitud FROM usuarios ORDER BY correo")
    assert len(rows) >= 8
    assert all(row["BCRYPT_PREFIJO"] == "$2a$12$" and row["HASH_LONGITUD"] == "60" for row in rows)
    rows = sql_query("propiedad-prendas-h2", "SELECT p.id, p.creador_id, u.correo, u.rol FROM prendas_autor p JOIN usuarios u ON u.id=p.creador_id WHERE p.eliminado=FALSE ORDER BY p.id")
    assert len(rows) >= 4 and all(row["ROL"] == "VENDEDOR" for row in rows)
    rows = sql_query("propiedad-bolsas-h2", "SELECT b.id, b.cliente_id, u.correo, u.rol FROM bolsas_estilo b JOIN usuarios u ON u.id=b.cliente_id WHERE b.eliminado=FALSE ORDER BY b.id")
    assert len(rows) >= 3 and all(row["ROL"] == "COMPRADOR" for row in rows)
    urllib.request.urlopen(h2 + "logout.do?jsessionid=" + session, timeout=30).close()

    evidence["resultado"] = "OK: registro, login, JWT, roles, propiedad, consultas públicas, OpenAPI y H2 comprobados por HTTP real."
    evidence["muestras"] = {"vendedor": seller["id"], "comprador": buyer["id"], "prenda": pid, "bolsa": bid}
    (output / "verificacion.json").write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"resultado": "OK", "peticionesHttp": len(evidence["http"]), "consultasSql": len(evidence["sql"]),
                      "baseUrl": base, "evidencia": str(output / "verificacion.json")}, ensure_ascii=False))


if __name__ == "__main__":
    main()
