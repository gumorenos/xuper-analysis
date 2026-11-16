#!/usr/bin/env python3
"""Login v8 con credenciales del usuario (.creds). No imprime secretos."""
import sys, hashlib, base64, json, os
sys.path.insert(0, "/home/gumorenos/xuper-repo/tools")
from portal_client import call

# AES-128-ECB con la clave de 16 bytes del app (k2/a.java)
try:
    from Cryptodome.Cipher import AES as _AES
    HAS_CRYPTO = True
except ImportError:
    HAS_CRYPTO = False

AES_KEY = b"ntFT65w6itH!lHCP"
SALT = "ntFT65w6itH!lHCPw7D=@qnsFC5adD28"

def aes_encrypt_b64(plain: str) -> str:
    data = plain.encode()
    pad = 16 - (len(data) % 16)
    data += bytes([pad]) * pad
    c = _AES.new(AES_KEY, _AES.MODE_ECB)
    return base64.b64encode(c.encrypt(data)).decode()

def combined_password(pwd: str) -> str:
    """k2.b.c = i(str) + a.a(str): md5(pwd+salt) + base64(AES-ECB-128(pwd))."""
    md5 = hashlib.md5((pwd + SALT).encode()).hexdigest()
    return (md5 + aes_encrypt_b64(pwd)).strip()

def read_creds(path="/home/gumorenos/xuper-repo/.creds"):
    user = pwd = ""
    for line in open(path):
        line = line.strip()
        if not line or line.startswith("#"): continue
        if "=" in line:
            k, v = line.split("=", 1)
            k = k.strip().lower()
            if k in ("user", "usuario", "username", "email"): user = v.strip()
            elif k in ("pass", "password", "contrasena", "contraseña"): pwd = v.strip()
        else:
            if not user: user = line
            elif not pwd: pwd = line
    return user, pwd

def main():
    if not HAS_CRYPTO:
        print("FALTA pycryptodomex en este interprete"); return
    user, pwd = read_creds()
    if not user or not pwd:
        print("creds incompletas: user?", bool(user), "pass?", bool(pwd)); return
    print(f"credenciales leidas: usuario='{user[:3]}***' largo_pass={len(pwd)}")
    pw_hash = combined_password(pwd)
    payload = {
        "accountType": "1", "userName": user, "password": pw_hash, "type": "1",
        "channel": "default",
    }
    out = call("/api/portalCore/v8/login", payload)
    safe = dict(out)
    if "data" in safe: safe["data"] = f"<{len(str(safe['data']))} chars>"
    dd = safe.pop("data_decrypted", None)
    print(json.dumps(safe, ensure_ascii=False, indent=1)[:600])
    if isinstance(dd, dict):
        dd.pop("userToken", None)
        print("LOGIN OK?" , bool(dd.get("userId")), "| userId:", dd.get("userId"), "| portalCode:", dd.get("portalCode"))
        print("campos respuesta:", list(dd.keys())[:20])
        # guardar sesion sin el token
        ses = {k: v for k, v in dd.items() if k != "userToken"}
        open("/home/gumorenos/xuper-repo/data/session.json", "w").write(json.dumps(ses, ensure_ascii=False, indent=1))
        print("sesion guardada en data/session.json (sin userToken)")

if __name__ == "__main__":
    main()
