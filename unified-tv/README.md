# UniPlay TV — app unificada (réplica multi-fuente de la familia Xuper/coolx)

Un solo APK para Android TV con **fuentes de datos intercambiables**:

| Fuente | Qué aporta | Estado |
|---|---|---|
| `source-coolx` | Panel del operador (auth, QR login, catálogo 1068 canales, VOD, cupones) — protocolo 3DES resuelto | Cliente Kotlin portado; gate de versión pendiente |
| `source-m3u` | Listas M3U/M3U8 externas (ej. la del app original) | Parser incluido |
| `source-direct` | Streams fijos tipo XuperAmigOnly (barinaslive, tvplusgratis…) | Cargados del catálogo descifrado |
| `sports` | ESPN API + marcador (estilo DeporWiwy) | Planeado |
| `ota` | Auto-update con manifest firmado | Planeado |

## Compilar

```bash
# requiere JDK 17, Android SDK (platform 34, build-tools 34) y gradle 8.x
export ANDROID_HOME=$HOME/android-sdk
/opt/gradle-8.7/bin/gradle -p unified-tv assembleDebug
# APK: unified-tv/build/outputs/apk/debug/
adb install -r unified-tv/build/outputs/apk/debug/*.apk
```

## Estructura

```
unified-tv/
 └─ app/src/main/java/com/uniplay/tv/
     ├─ MainActivity.kt        (lista de canales + selector de fuente)
     ├─ PlayerActivity.kt      (ExoPlayer/Media3)
     ├─ model/Channel.kt
     ├─ source/Source.kt       (interfaz de fuente)
     ├─ source/CoolxSource.kt  (protocolo portalCore: envelope 3DES/ECB->hex)
     ├─ source/M3uSource.kt
     ├─ source/DirectSource.kt (streams estáticos cosechados)
     └─ util/Crypto.kt         (replica del 3DES + decoder base64 del APK)
```

## Protocolo coolx (resumen operativo)

- Clave runtime = `dc_b64(UUID_del_build)[:24]`; UUID = PORTAL_KEY descifrado con `*&@!6d5d-c483-4720-bb29-785b8f321c^%`
- Body = JSON(bean + identidad) → 3DES/ECB/PKCS5 → **hex** (POST, Content-Type json)
- Respuesta = `{"returnCode","errorMessage","data":"<hex>"}`; `data` se descifra igual
- Headers: `apk`, `apkVer`, `spkgVer`; identidad: loginType=2, appLanguage, apkVersion, sysVersion, appId, hardwareInfo, model, product, cpu, B29(serial cifrado), reserve1(MAC gateway cifrada), portalCode, deviceToken, sn, drmId, sdkVer
- Referencia Python funcional: `tools/portal_client.py`
