#!/bin/bash
# Patch opcional del decode: logea body (WXBODY) y URL (WXURL) en logcat.
# $1 = dir del apk decodificado
set -e
APK="$1"
[ -f "$APK/smali_classes2/zb/c.smali" ] || exit 0

python3 - "$APK/smali_classes2/zb/c.smali" <<'EOF'
import sys
p = sys.argv[1]
s = open(p).read()

# --- Patch 1: WXBODY (log del JSON plano en zb.c.b antes del return) ---
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

# --- Patch 2: WXURL (log de la request en el inicio de intercept) ---
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
        print("patron no encontrado en intercept; continuo solo con WXBODY")
    seg = seg.replace(marker, injected, 1)
    s = s[:i] + seg + s[j:]
    print("patched WXURL")
else:
    print("WXURL ya presente")

open(p, "w").write(s)
EOF
