#!/usr/bin/env python3
"""Cliente de prueba del protocolo portalCore (coolx) — build WiwyXuper v4.34.4.

Esquema (verificado en código):
  - clave runtime = b64decode(PORTAL_KEY_del_manifiesto)[:24]  (3DES/ECB/PKCS5)
  - request:  body JSON -> 3DES-ECB -> hex -> {"data": <hex>, "len": <len(json original)>}
  - response: {"returnCode","errorMessage","data": <hex>} -> data se descifra igual
"""
import base64, json, subprocess, sys

PORTAL = "https://fuxok.nguvmqhpk.com"
PORTAL_KEY_HEX = "346f53476b586674612b4b66324e497a6f726863434832554f6c37597433615275667a582f6e556b634f4b3538304f2b3748567573673d3d"

def runtime_key():
    return base64.b64decode(PORTAL_KEY_HEX)[:24]

def des3(decrypt: bool, data: bytes, key: bytes):
    args = ["openssl", "des-ede3", "-K", key.hex(), "-nosalt"]
    args += (["-d", "-nopad"] if decrypt else ["-e"])
    p = subprocess.run(args, input=data, capture_output=True)
    if p.returncode != 0:
        raise RuntimeError(p.stderr.decode()[:200])
    return p.stdout

def pkcs5_pad(b):
    n = 8 - (len(b) % 8)
    return b + bytes([n]) * n

def pkcs5_unpad(b):
    if not b:
        return b
    n = b[-1]
    return b[:-n] if 1 <= n <= 8 and b[-n:] == bytes([n]) * n else b

def encrypt_payload(obj) -> dict:
    key = runtime_key()
    js = json.dumps(obj, separators=(",", ":"), ensure_ascii=False).encode()
    ct = des3(False, pkcs5_pad(js), key)
    return {"data": ct.hex(), "len": len(js)}

def decrypt_data(hexdata) -> str:
    key = runtime_key()
    if len(hexdata) % 2:
        hexdata = "0" + hexdata
    raw = bytes.fromhex(hexdata)
    pt = des3(True, raw, key)
    return pkcs5_unpad(pt).decode(errors="replace")

def call(endpoint, payload_obj, method="POST"):
    import urllib.request
    body = json.dumps(encrypt_payload(payload_obj)).encode()
    req = urllib.request.Request(
        f"{PORTAL}{endpoint}", data=body, method=method,
        headers={"Content-Type": "application/json;charset=utf-8",
                 "Cache-Control": "no-store",
                 "apk": "com.android.msandroid",
                 "apkVer": "43404",
                 "spkgVer": "2026-10-18 10:23:33_34_14_unknown",
                 "User-Agent": "okhttp/4.9.3"})
    with urllib.request.urlopen(req, timeout=20) as r:
        resp = json.loads(r.read().decode())
    out = dict(resp)
    if resp.get("data"):
        try:
            out["data_decrypted"] = json.loads(decrypt_data(resp["data"]))
        except Exception as e:
            out["data_decrypted_err"] = str(e)
            out["data_decrypted_raw"] = decrypt_data(resp["data"])[:400]
    return out

if __name__ == "__main__":
    ep = sys.argv[1] if len(sys.argv) > 1 else "/api/portalCore/config/get"
    payload = json.loads(sys.argv[2]) if len(sys.argv) > 2 else {}
    try:
        print(json.dumps(call(ep, payload), ensure_ascii=False, indent=1)[:3000])
    except Exception as e:
        print("ERROR:", e)
