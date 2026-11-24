#!/bin/bash
# Patch opcional del decode: logea body (WXBODY), url/headers (WXURL/WXHDR)
# y crypto (WXCENC/WXCKEY en fc/b.smali).
# $1 = dir del apk decodificado
set -e
APK="$1"

# --- zb/c.smali: WXBODY + WXURL + WXHDR ---
C="$APK/smali_classes2/zb/c.smali"
[ -f "$C" ] || exit 0

python3 - "$C" <<'EOF'
import sys
p = sys.argv[1]
s = open(p).read()

if "WXBODY" not in s:
    i = s.find(".method public static b(Lokhttp3/Request;)")
    j = s.find(".end method", i)
    body = s[i:j]
    k = body.rfind("return-object p0")
    if k == -1:
        print("no se encontró return en b(); abort body"); sys.exit(1)
    patched = body[:k] + '    const-string v3, "WXBODY"\n    invoke-static {v3, p0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n\n' + body[k:]
    s = s[:i] + patched + s[j:]
    print("patched WXBODY")
else:
    print("WXBODY ya presente")

if "WXURL" not in s:
    marker = "    const-string v0, \"userId\"\n"
    injected = """    invoke-interface {p1}, Lokhttp3/Interceptor$Chain;->request()Lokhttp3/Request;
    move-result-object v8
    invoke-virtual {v8}, Lokhttp3/Request;->url()Lokhttp3/HttpUrl;
    move-result-object v8
    invoke-virtual {v8}, Lokhttp3/HttpUrl;->toString()Ljava/lang/String;
    move-result-object v8
    const-string v7, "WXURL"
    invoke-static {v7, v8}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    const-string v0, "userId"
"""
    i = s.find(".method public intercept(Lokhttp3/Interceptor$Chain;)Lokhttp3/Response;")
    j = s.find(".end method", i)
    seg = s[i:j]
    if marker not in seg:
        print("patron no encontrado en intercept; solo WXBODY")
    else:
        seg = seg.replace(marker, injected, 1)
        s = s[:i] + seg + s[j:]
        print("patched WXURL")
else:
    print("WXURL ya presente")

open(p, "w").write(s)
EOF

# --- fc/b.smali: log de crypto (clave + entrada en cifrar) ---
F="$APK/smali/fc/b.smali"
[ -f "$F" ] || exit 0

python3 - "$F" <<'EOF'
import sys
p = sys.argv[1]
s = open(p).read()

if "WXCENC" not in s:
    marker = """    .locals 3

    .line 1
    const-string v0, ""

    .line 2
    .line 3
    new-instance v1, Ldc/a;
"""
    injected = """    .locals 3

    const-string v0, "WXCENC"
    invoke-static {v0, p0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    const-string v0, "WXCKEY"
    invoke-static {v0, p1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 1
    const-string v0, ""

    .line 2
    .line 3
    new-instance v1, Ldc/a;
"""
    i = s.find(".method public static b(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")
    j = s.find(".end method", i)
    seg = s[i:j]
    if marker not in seg:
        print("patron cifrado no encontrado")
    else:
        seg = seg.replace(marker, injected, 1)
        s = s[:i] + seg + s[j:]
        print("patched WXCENC")
else:
    print("WXCENC ya presente")

open(p, "w").write(s)
EOF

# --- fc/b.smali: log de descifrado de respuestas (WXDECIN/KEY/OUT) ---
F2="$APK/smali/fc/b.smali"
[ -f "$F2" ] || exit 0

python3 - "$F2" <<'EOF'
import sys
p = sys.argv[1]
s = open(p).read()
m_head = ".method public static a(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;\n    .locals 2\n"
inj_head = m_head + '    const-string v0, "WXDECIN"\n    invoke-static {v0, p0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n    const-string v0, "WXDECKEY"\n    invoke-static {v0, p1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n'
i = s.find(".method public static a(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")
j = s.find(".end method", i)
seg = s[i:j]
anchor_end = "    invoke-direct {p1, p0, v0}, Ljava/lang/String;-><init>([BLjava/lang/String;)V"
k = seg.find(anchor_end)
if k == -1:
    print("anchor_end no match en a(); sin WXDEC"); sys.exit(0)
kret = seg.find("return-object p1", k)
seg2 = seg[:kret] + '    const-string v0, "WXDECOUT"\n    invoke-static {v0, p1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n' + seg[kret:]
s = s[:i] + seg2 + s[j:]
if "WXDECIN" not in s:
    s = s.replace(m_head, inj_head, 1)
open(p, "w").write(s)
print("patched WXDEC")
EOF

# --- okhttp3/RealCall.smali: log de TODAS las llamadas+respuestas okhttp ---
R="$APK/smali/okhttp3/RealCall.smali"
[ -f "$R" ] || exit 0

python3 - "$R" <<'EOF'
import sys
p = sys.argv[1]
s = open(p).read()

if "WXCALL" not in s:
    m_head = ".method getResponseWithInterceptorChain()Lokhttp3/Response;\n    .locals 13\n"
    inj_head = m_head + '    const-string v1, "WXCALL"\n    iget-object v2, p0, Lokhttp3/RealCall;->originalRequest:Lokhttp3/Request;\n    invoke-virtual {v2}, Lokhttp3/Request;->url()Lokhttp3/HttpUrl;\n    move-result-object v2\n    invoke-virtual {v2}, Lokhttp3/HttpUrl;->toString()Ljava/lang/String;\n    move-result-object v2\n    invoke-static {v1, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n'
    i = s.find(m_head)
    if i == -1:
        print("metodo getResponse no encontrado; sin WXCALL")
    else:
        s = s.replace(m_head, inj_head, 1)
        print("patched WXCALL (head)")

if "WXRESP2" not in s:
    import re as _re
    pat = _re.compile(r"(    if-nez v1, :cond_1\n)(\n    \.line \d+\n)+(\n?    return-object v0\n)")
    m = pat.search(s)
    if not m:
        print("anchor resp (regex) no encontrado; sin WXRESP2")
    else:
        inj = "    const-wide/16 v1, 0x10000\n    invoke-virtual {v0, v1, v2}, Lokhttp3/Response;->peekBody(J)Lokio/BufferedSource;\n    move-result-object v1\n    invoke-virtual {v1}, Lokio/BufferedSource;->readUtf8()Ljava/lang/String;\n    move-result-object v1\n    const-string v2, \"WXRESP2\"\n    invoke-static {v2, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n"
        s = s[:m.end(1)] + inj + s[m.end(1):]
        print("patched WXRESP2 (regex)")

open(p, "w").write(s)
EOF
