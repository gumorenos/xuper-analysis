import subprocess, os

TBL = {}
alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
for i, c in enumerate(alpha):
    TBL[ord(c)] = i

def b64dec_dc(s):
    """Replica de dc.a/ec.c: atomos de 4 chars; invalidos valen -1; '=' encoge atomo."""
    out = bytearray()
    i = 0
    n = len(s)
    while True:
        if i >= n:
            break
        atom = s[i:i+4]
        i += 4
        if len(atom) < 2:
            break  # not enough bytes for atom -> exhausted (ec.b) -> stop
        # read exactly 4 chars like j(): fewer available -> partial atom
        size = len(atom)
        if size < 2:
            break
        if size > 3 and atom[3] == '=':
            size = 3
        if size > 2 and atom[2] == '=':
            size = 2
        v = [TBL.get(ord(ch), -1) for ch in atom]
        v0, v1, v2, v3 = v[0], v[1], v[2], v[3]
        def m(x, mask): return (x & 0xFF) & mask if x >= 0 else ((-1) & 0xFF) & mask
        # standard base64 bit math with Java-like masking on -1
        def sh(x, k): return (x << k) & 0xFF if x >= 0 else (((-1) << k) & 0xFF)
        if size == 2:
            out.append((sh(v0,2) & 0xFC) | ((v1 >> 4) & 0x03) if v1>=0 else (sh(v0,2) & 0xFC) | 0x03)
        elif size == 3:
            out.append((sh(v0,2) & 0xFC) | ((v1 >> 4) & 0x03) if v1>=0 else (sh(v0,2) & 0xFC) | 0x03)
            out.append((sh(v1,4) & 0xF0) | ((v2 >> 2) & 0x0F) if v2>=0 else (sh(v1,4) & 0xF0) | 0x0F)
        else:
            out.append((sh(v0,2) & 0xFC) | ((v1 >> 4) & 0x03) if v1>=0 else (sh(v0,2) & 0xFC) | 0x03)
            out.append((sh(v1,4) & 0xF0) | ((v2 >> 2) & 0x0F) if v2>=0 else (sh(v1,4) & 0xF0) | 0x0F)
            out.append((sh(v2,6) & 0xC0) | (v3 & 0x3F) if v3>=0 else (sh(v2,6) & 0xC0) | 0x3F)
    return bytes(out)

KEY_STR = "*&@!6d5d-c483-4720-bb29-785b8f321c^%"
key_material = b64dec_dc(KEY_STR)
key24 = key_material[:24]
print("key bytes (27):", key_material.hex())
print("key24:", key24.hex())

def des3_ecb_decrypt(ct, key):
    p = subprocess.run(["openssl","des3","-d","-K",key.hex(),"-iv","0000000000000000","-nosalt"],
                       input=ct, capture_output=True)
    return p.stdout, p.stderr

def pkcs5_strip(b):
    if not b: return b
    p = b[-1]
    return b[:-p] if 1 <= p <= 8 and b[-p:] == bytes([p])*p else b

domains = {
 "portal_main":   "7351502b4a6f46686d63416370686c716572705a32337842666a365a4c356c47",
 "portal_backup": "6b6673704d4f765539694d734f553352446d503456337842666a365a4c356c47",
 "epg_main":      "53747a5a6372792f7532767444523036505536764d337842666a365a4c356c47",
 "epg_backup":    "63766b6b74637356387a36777069646f306f7839336e7842666a365a4c356c47",
 "upgrade_main":  "4d4161356c67367775566c6b68537747394f436678337842666a365a4c356c47",
 "upgrade_backup":"4d512f3259317135557a7362794d6b304f586d6866487842666a365a4c356c47",
 "notice_main":   "3170695a7437777a734b7a74373231675444573952487842666a365a4c356c47",
 "notice_backup": "566256575268494e56392f54427178543266453571487842666a365a4c356c47",
 "ad_main":       "717a4d65706b6d2f4b7348646b526c7868304e39726e7842666a365a4c356c47",
 "ad_backup":     "6641466b4332302b7061554c4f4f6668733273506e487842666a365a4c356c47",
 "bigbee_main":   "644a786e35433533734b685473535258506e2b4b5a487842666a365a4c356c47",
 "bigbee_backup": "45526d656c39313264565158343373536733392f71487842666a365a4c356c47",
 "scan_qr_login": "795066316266327a7a574c354e53504472505559756e7842666a365a4c356c47",
}
import binascii
for name, hexval in domains.items():
    b64str = binascii.unhexlify(hexval).decode()
    ct = bytes(b64dec_dc(b64str))
    pt, err = des3_ecb_decrypt(ct, key24)
    print(f"{name:15s} ct={len(ct)}B  pt={pkcs5_strip(pt)!r}")

print("\n--- variante: key = primeros 24 chars ASCII crudos ---")
import binascii
def try_key(k24, label):
    ok = True
    for name, hexval in list(domains.items())[:3]:
        b64str = binascii.unhexlify(hexval).decode()
        ct = bytes(b64dec_dc(b64str))
        pt, err = des3_ecb_decrypt(ct, k24)
        pr = pkcs5_strip(pt)
        print(f"  {label} {name}: {pr!r}")
        if not all(32 <= b < 127 for b in pr): ok = False
    return ok

try_key(KEY_STR[:24].encode(), "raw24")
try_key(key_material[3:27], "skip-first-atom")
