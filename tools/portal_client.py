#!/usr/bin/env python3
"""Cliente del protocolo portalCore (coolx) — build WiwyXuper v4.34.4.

Receta (verificada en zb/c.java + zb/b.java + fc/b.java):
  Body  = JSON(bean) + campos de identidad (zb.b.a())
        -> 3DES/ECB/PKCS5 con clave runtime -> HEX (el body completo va cifrado)
  Respuesta = {"returnCode","errorMessage","data":"<hex>"}; "data" se descifra igual.
  Clave runtime = b64decode(PORTAL_KEY_hex_del_manifest)[:24]
  (el formato {"data","len"} era del sistema de descargas, NO del portal)
"""
import base64, binascii, json, subprocess, sys, urllib.request

PORTAL = "https://brkhzs.udigkkrc.com"
PORTAL_KEY_HEX = "346f53476b586674612b4b66324e497a6f726863434832554f6c37597433615275667a582f6e556b634f4b3538304f2b3748567573673d3d"
SN = "c05fdf3595ba2ec9f49486f20a72ef06"  # SN del box (registrado en el portal)
# PORTAL_KEY descifra (3DES/ECB, clave estática *&@!6d5d...) a la UUID del build:
BUILD_UUID = "568e194f-d170-4369-96e8-4a457f641880"
PORTALCODE = BUILD_UUID  # valor enviado como portalCode (candidato)

_TBL = {}
_alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
for _i, _c in enumerate(_alpha):
    _TBL[ord(_c)] = _i

def dc_b64(s):
    """Replica del decoder base64 del APK (dc.a): átomos de 4; inválidos -> -1; '=' encoge."""
    out = bytearray(); i = 0
    while i < len(s):
        atom = s[i:i+4]; i += 4
        if len(atom) < 2: break
        size = len(atom)
        if size > 3 and atom[3] == "=": size = 3
        if size > 2 and atom[2] == "=": size = 2
        v = [_TBL.get(ord(ch), -1) for ch in atom]
        v0, v1, v2, v3 = v
        def sh(x, k): return (x << k) & 0xFF
        def m1(x): return 0xFF if x < 0 else x
        out.append((sh(m1(v0), 2) & 0xFC) | (3 if v1 < 0 else (v1 >> 4) & 3))
        if size >= 3:
            out.append((sh(m1(v1), 4) & 0xF0) | (15 if v2 < 0 else (v2 >> 2) & 15))
        if size >= 4:
            out.append((sh(m1(v2), 6) & 0xC0) | (63 if v3 < 0 else v3 & 63))
    return bytes(out)

def runtime_key():
    # clave runtime = decodificador del APK sobre la UUID del build
    return dc_b64(BUILD_UUID)[:24]

def des3(decrypt, data):
    key = runtime_key()
    args = ["openssl", "des-ede3", "-K", key.hex(), "-nosalt"]
    args += (["-d", "-nopad"] if decrypt else ["-e"])
    p = subprocess.run(args, input=data, capture_output=True)
    if p.returncode != 0:
        raise RuntimeError(p.stderr.decode()[:200])
    return p.stdout

def pad(b):
    n = 8 - (len(b) % 8)
    return b + bytes([n]) * n

def unpad(b):
    if not b:
        return b
    n = b[-1]
    return b[:-n] if 1 <= n <= 8 and b[-n:] == bytes([n]) * n else b

def hexenc(b):  # fc.a.c
    return ''.join(f"{c:02x}" for c in b)

def hexdec(s):  # fc.a.b
    if len(s) % 2:
        s = "0" + s
    return binascii.unhexlify(s).decode("utf-8", "replace")

BRANDKEY = dc_b64("combrasiltvaslgklxckbcombrasiltv")  # 24 bytes exactos

def brand_encrypt(plain: str) -> str:
    """j.d() = fc.b.b: 3DES/ECB/PKCS5 -> b64 -> hex (chars)."""
    ct = des3_bytes(False, pad(plain.encode()))
    return hexenc(base64.b64encode(ct))

def des3_bytes(decrypt, data):
    key = BRANDKEY
    args = ["openssl", "des-ede3", "-K", key.hex(), "-nosalt"]
    args += (["-d", "-nopad"] if decrypt else ["-e"])
    p = subprocess.run(args, input=data, capture_output=True)
    if p.returncode != 0:
        raise RuntimeError(p.stderr.decode()[:200])
    return p.stdout

IDENTITY = {
    "loginType": "2",
    "appLanguage": "es",
    "apkVersion": "43476",
    "sysVersion": "2025-11-06 18:02:55_34_14_5.15.170-android14-11-maybe-dirty",
    "appId": "com.android.msandroid",
    "hardwareInfo": "amlogic",
    "model": "B866VH",
    "product": "B866VH",
    "cpu": "armeabi-v7a",
    "B29": brand_encrypt("ZTEATV45503609797"),
    "reserve1": brand_encrypt("d0ea11a7e816"),
    "portalCode":"masnew",
    "deviceToken": "",
    "sn": SN,
    "drmId": "",
    "sdkVer": 34,
}

def call(endpoint, payload=None):
    body_obj = dict(payload or {})
    body_obj.update(IDENTITY)
    js = json.dumps(body_obj, separators=(",", ":"), ensure_ascii=False).encode()
    ct = des3(False, pad(js))
    # el app envia el body LIMPIO (sin identidad mergeada)
    req = urllib.request.Request(
        PORTAL + endpoint, data=hexenc(ct).encode(), method="POST",
        headers={
            "Content-Type": "application/json;charset=utf-8",
            "apk": "com.android.msandroid",
            "apkVer": "43404",
            "spkgVer": "2025-11-06 18:02:55_34_14_",
            "NoLog": "true",
            "User-Agent": "okhttp/4.9.3",
        })
    with urllib.request.urlopen(req, timeout=20) as r:
        resp = json.loads(r.read().decode())
    out = dict(resp)
    d = resp.get("data")
    if isinstance(d, str) and d:
        try:
            out["data_decrypted"] = json.loads(unpad(des3(True, hexdec(d).encode())))
        except Exception as e:
            out["data_decrypted_err"] = str(e)
    return out

if __name__ == "__main__":
    ep = sys.argv[1] if len(sys.argv) > 1 else "/api/portalCore/getBaseTime"
    payload = json.loads(sys.argv[2]) if len(sys.argv) > 2 else {}
    try:
        print(json.dumps(call(ep, payload), ensure_ascii=False, indent=1)[:2500])
    except Exception as e:
        print("ERROR:", e)
