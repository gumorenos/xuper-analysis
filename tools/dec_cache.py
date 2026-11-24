import base64, binascii, subprocess, sys

TBL = {}
_alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
for i, c in enumerate(_alpha):
    TBL[ord(c)] = i

def dcb64(s):
    out = bytearray(); i = 0
    while i < len(s):
        atom = s[i:i+4]; i += 4
        if len(atom) < 2: break
        size = len(atom)
        if size > 3 and atom[3] == "=": size = 3
        if size > 2 and atom[2] == "=": size = 2
        v = [TBL.get(ord(ch), -1) for ch in atom]
        v0, v1, v2, v3 = v
        sh = lambda x, k: (x << k) & 0xFF
        m1 = lambda x: 0xFF if x < 0 else x
        out.append((sh(m1(v0), 2) & 0xFC) | (3 if v1 < 0 else (v1 >> 4) & 3))
        if size >= 3:
            out.append((sh(m1(v1), 4) & 0xF0) | (15 if v2 < 0 else (v2 >> 2) & 15))
        if size >= 4:
            out.append((sh(m1(v2), 6) & 0xC0) | (63 if v3 < 0 else v3 & 63))
    return bytes(out)

KEY = dcb64("b700bce0-91c7-47df-a593-747ae941bf34")[:24]

def des3d(data):
    p = subprocess.run(["openssl", "des-ede3", "-d", "-K", KEY.hex(), "-nosalt", "-nopad"], input=data, capture_output=True)
    return p.stdout

raw = open(sys.argv[1]).read().strip()
if len(raw) % 2: raw = "0" + raw
b64 = binascii.unhexlify(raw).decode()
pt = des3d(base64.b64decode(b64))
n = pt[-1]
if 1 <= n <= 8 and pt[-n:] == bytes([n]) * n:
    pt = pt[:-n]
txt = pt.decode("utf-8", "replace")
print(txt[:800])
open(sys.argv[1] + ".json", "w").write(txt)
print(f"\n[guardado {sys.argv[1]}.json — {len(txt)} chars]")
