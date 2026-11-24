#!/bin/bash
# Repack de un APK coolx con debuggable=true (para análisis run-as)
# Uso: repack.sh <apk_original> <salida.apk>
set -e
APK_IN="$1"
APK_OUT="$2"
[ -n "$APK_IN" ] || { echo "uso: repack.sh <apk> <salida>"; exit 1; }

APKTOOL="${APKTOOL:-/home/gumorenos/.config/craft-anything/engines/apktool/apktool.jar}"
AAPT2="${AAPT2:-aapt2}"
WORK=$(mktemp -d)
trap 'echo "== FALLO =="; tail -30 "$WORK/build.log" 2>/dev/null; tail -10 "$WORK/decode.log" 2>/dev/null' ERR

echo "[1/4] decode"
java -jar "$APKTOOL" d -f -o "$WORK/apk" "$APK_IN" 2>&1 | tail -3

echo "[1.5] patch smali"
if [ -f "$(dirname "$0")/patch.sh" ]; then "$(dirname "$0")/patch.sh" "$WORK/apk"; fi

echo "[2/4] patch manifest + NSC"
sed -i 's|<application|<application android:debuggable="true"|' "$WORK/apk/AndroidManifest.xml"
grep -q 'android:debuggable="true"' "$WORK/apk/AndroidManifest.xml"
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

# renombrar recursos con $ (aapt2 no los acepta)
find "$WORK/apk/res" -name '*$*' -exec bash -c 'mv "$1" "${1//\$/d_}"' _ {} \; 2>/dev/null || true
grep -rl '\$' "$WORK/apk/res" --include='*.xml' | xargs -r sed -i 's/\$avd/d_avd/g; s/\$ic_launcher/d_ic_launcher/g' || true

echo "[3/4] rebuild (aapt2 nativo)"
java -jar "$APKTOOL" b --aapt "$AAPT2" "$WORK/apk" -o "$WORK/unsigned.apk" 2>&1 | tail -5

echo "[4/4] firma"
KS="$WORK/debug.keystore"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -alias debug -storepass android \
    -keypass android -dname "CN=Android Debug,O=UniPlay,C=US" \
    -keyalg RSA -keysize 2048 -validity 10000 2>/dev/null
APKSIGNER=$(find "${ANDROID_HOME:-/usr/lib/android-sdk}/build-tools" -name apksigner 2>/dev/null | sort | tail -1)
[ -x "$APKSIGNER" ] || APKSIGNER=$(dirname "$(readlink -f "$(which keytool)")")/apksigner
[ -x "$APKSIGNER" ] || APKSIGNER=apksigner
echo "apksigner: $APKSIGNER"
"$APKSIGNER" sign --ks "$KS" --ks-pass pass:android --out "$APK_OUT" "$WORK/unsigned.apk"
ls -la "$APK_OUT"
echo "OK: $APK_OUT"
