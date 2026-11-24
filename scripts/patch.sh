#!/bin/bash
# Patch opcional del decode: aplica parches de smali para diagnóstico.
# $1 = dir del apk decodificado
set -e
APK="$1"
[ -f "$APK/smali_classes2/zb/c.smali" ] || exit 0

python3 - "$APK/smali_classes2/zb/c.smali" <<'EOF'
import sys
p = sys.argv[1]
s = open(p).read()
if "WXBODY" in s:
    print("ya parcheado"); sys.exit(0)
old = """    move-result-object p0

    .line 57
    return-object p0
.end method
"""
new = """    move-result-object p0

    const-string v3, "WXBODY"
    invoke-static {v3, p0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    return-object p0
.end method
"""
# solo en el método b (primer return-object tras readString)
if old in s:
    s = s.replace(old, new, 1)
    open(p, "w").write(s)
    print("patched WXBODY en zb/c.smali")
else:
    # fallback: insertar antes del primer return-object del método b
    i = s.find(".method public static b(Lokhttp3/Request;)")
    j = s.find(".end method", i)
    body = s[i:j]
    k = body.rfind("return-object p0")
    if k == -1:
        print("no se encontró return en b(); ABORT"); sys.exit(1)
    patched = body[:k] + '    const-string v3, "WXBODY"\n    invoke-static {v3, p0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I\n\n' + body[k:]
    s = s[:i] + patched + s[j:]
    open(p, "w").write(s)
    print("patched WXBODY (fallback) en zb/c.smali")
EOF
