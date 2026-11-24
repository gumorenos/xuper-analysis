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
old = """    .line 1
    const-string v0, "userId"
    .line 2
"""
new = """    .line 1
    invoke-interface {p1}, Lokhttp3/Interceptor$Chain;->request()Lokhttp3/Request;
    move-result-object v8
    invoke-virtual {v8}, Lokhttp3/Request;->url()Lokhttp3/HttpUrl;
    move-result-object v8
    invoke-virtual {v8}, Lokhttp3/HttpUrl;->toString()Ljava/lang/String;
    move-result-object v8
    const-string v7, "WXURL"
    invoke-static {v7, v8}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    invoke-virtual {v8}, Ljava/lang/String;->getBytes()[B
    move-result-object v8
    invoke-virtual {v8}, [B->length
    move-result v8
    new-instance v6, Ljava/lang/StringBuilder;
    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V
    const-string v7, "LEN="
    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v6, v8}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v8
    const-string v7, "WXURL"
    invoke-static {v7, v8}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    const-string v0, "userId"
    .line 2
"""
i = s.find('.method public intercept(Lokhttp3/Interceptor$Chain;)Lokhttp3/Response;')
j = s.find('.end method', i)
seg = s[i:j]
if 'WXURL' in s:
    print('ya parcheado'); sys.exit(0)
seg2 = seg.replace(old, new, 1)
if seg2 == seg:
    print('patron no encontrado en intercept'); sys.exit(1)
s = s[:i] + seg2 + s[j:]
open(p, 'w').write(s)
print('patched WXURL en intercept')
EOF
