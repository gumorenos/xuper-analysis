"""Addon mitmproxy: registra peticiones/respuestas del protocolo Xuper (portalCore).

Uso:
  ~/.local/bin/mitmdump -s dump_portal.py -p 8888 --set block_global=false
Salida: /home/gumorenos/xuper-capture/captura.jsonl (1 línea por request)
"""
import json, time, os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "captura.jsonl")
FILTRO = ("portalCore", "adserver", "notice", "config", "PreInstall", "subs")

def request(flow):
    if not any(k in flow.request.pretty_url for k in FILTRO):
        return
    rec = {
        "ts": time.time(),
        "dir": "req",
        "method": flow.request.method,
        "url": flow.request.pretty_url,
        "host": flow.request.host,
        "port": flow.request.port,
        "headers": dict(flow.request.headers),
        "body": flow.request.get_text(strict=False)[:20000],
    }
    with open(OUT, "a") as f:
        f.write(json.dumps(rec, ensure_ascii=False) + "\n")

def response(flow):
    if not any(k in flow.request.pretty_url for k in FILTRO):
        return
    rec = {
        "ts": time.time(),
        "dir": "resp",
        "url": flow.request.pretty_url,
        "status": flow.response.status_code,
        "headers": dict(flow.response.headers),
        "body": flow.response.get_text(strict=False)[:20000],
    }
    with open(OUT, "a") as f:
        f.write(json.dumps(rec, ensure_ascii=False) + "\n")
