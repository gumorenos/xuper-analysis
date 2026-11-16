#!/bin/bash
# Repack de un APK coolx con debuggable=true (para análisis run-as)
# Uso: repack.sh <apk_original> <salida.apk>
# Requiere: java, apktool (jar), aapt2 nativo (x86), apksigner (JDK), keytool
set -e
APK_IN="$1"
APK_OUT="$2"
[ -z "$APK_IN" ] || [ -z "$APK_OUT" ] && { echo "uso: repack.sh <apk> <salida>"; exit 1; }

APKTOOL="${APKTOOL:-/home/gumorenos/.config/craft-anything/engines/apktool/apktool.jar}"
AAPT2="${AAPT2:-aapt2}"   # en CI, el aapt2 nativo
WORK=$(mktemp -d)

java -jar "$APKTOOL" d -f --use-aapt2 -o "$WORK/apk" "$APK_IN" > "$WORK/decode.log" 2>&1

# 1) debuggable
sed -i 's|<application|<application android:debuggable="true"|' "$WORK/apk/AndroidManifest.xml"
grep -q 'android:debuggable="true"' "$WORK/apk/AndroidManifest.xml"

# 2) NSC: confiar en CAs de usuario (para mitmproxy si se desea)
cat > "$WORK/apk/res/xml/network_security_config.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system"/>
            <certificates src="user"/>
        </trust-anchors>
    </base-config>
</network-security-config>
XML

# 3) rebuild
java -jar "$APKTOOL" b --use-aapt2 --aapt "$AAPT2" "$WORK/apk" -o "$WORK/unsigned.apk" > "$WORK/build.log" 2>&1

# 4) firmar (debug key)
KS="$WORK/debug.keystore"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -alias debug -storepass android \
    -keypass android -dname "CN=Android Debug,O=UniPlay,C=US" \
    -keyalg RSA -keysize 2048 -validity 10000 2>/dev/null
APKSIGNER=$(dirname "$(which keytool)")/apksigner
[ -x "$APKSIGNER" ] || APKSIGNER=apksigner
"$APKSIGNER" sign --ks "$KS" --ks-pass pass:android --out "$APK_OUT" "$WORK/unsigned.apk"
echo "OK: $APK_OUT"

# (log de fallos visible)
trap 'echo "FALLO: revisa $WORK/decode.log y $WORK/build.log"; tail -20 "$WORK/build.log" 2>/dev/null; tail -5 "$WORK/decode.log" 2>/dev/null' ERR
