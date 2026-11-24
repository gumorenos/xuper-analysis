# Estado del proyecto Xuper/UniPlay — 2026-11-24

## Resumen

Ingeniería inversa de la familia de apps IPTV coolx/Xuper (com.android.mgstv v4.34.5,
com.xuper.wiwyxuper v4.34.4) y construcción de la app propia UniPlay TV.

## Protocolo del portal (capturado y verificado)

Método: app original reempaquetada con logging smali (scripts/patch.sh) reconstruida en CI
(.github/workflows/repack.yml) — el logcat entrega bodies, respuestas y crypto en claro.

- **Sobre**: JSON plano → 3DES/ECB/PKCS5 con key = b64Propio(PORTAL_KEY)[:24] → hex como body.
  PORTAL_KEY vive en el meta-data del manifest ("568e194f-d170-4369-96e8-4a457f641880").
- **Clave de marca** (serial/MAC cifrados): "combrasiltvaslgklxckbcombrasiltv".
- **Headers**: `apk: com.android.msandroid`, `apkVer: 43404`,
  `spkgVer: 2025-11-06 18:02:55_34_14_`, `NoLog: true`, UA `okhttp/3.12.12`.
- **Identidad en el body** (merge del interceptor): loginType, appLanguage, apkVersion "49902",
  sysVersion, appId, hardwareInfo, model/product reales, cpu, B29 (serial cifrado),
  reserve1 (MAC gateway cifrada), portalCode "masnew", deviceToken FCM, sn, drmId Widevine, sdkVer 34.
- **Endpoints**: `device/updateOrInsert`, `v8/active` (asigna userId/userToken),
  `v15/getSlbInfo` (resolución de streams), `v3/getColumnContents`, `v6/getLiveData` (catálogo live).
- **Hosts** (DCS rota): fuxok.nguvmqhpk.com, brkhzs.udigkkrc.com, eslitx.7jpeq68c.com; EPG/DCC en
  vgwbm/rokbd/zxiws/nxiqj (HTTP plano).
- **Sesión**: userId 997630678 (estable por huella), userToken rota por instalación.

## El gate anti-piratería (resuelto el misterio)

`portal200001 "版本已停止使用"` NO es una respuesta del backend: es un bloqueo por **huella TLS**
en Cloudflare. Solo pasa el TLS de okhttp-Android (BoringSSL). Verificado: cliente Python/curl
con body, headers, clave, UA, HTTP/2 y gzip idénticos → bloqueado siempre; el app original →
`returnCode: 0, 成功`. Consecuencia: cualquier cliente legítimo del protocolo debe correr como
app Android con okhttp 3.12.

Además `getSlbInfo` estuvo temporalmente bloqueado por Cloudflare también para el app
(rate/WAF por path).

## Entregables en el repo

- `tools/portal_client.py` — cliente Python del protocolo (bloqueado por el gate TLS; sirve de referencia)
- `tools/dec_cache.py`, `tools/dec_portalcode.py` — descifradores de cachés y config
- `scripts/patch.sh` + `scripts/repack.sh` — pipeline de captura (smali logging WX*/WXRESPX)
- `scripts/repack.sh` + `.github/workflows/repack.yml` — repack reproducible en CI (apktool 3.0.3 + aapt2 nativo)
- `data/uniplay_catalog.json` — catálogo de 1.068 canales con playCodes, licencias y logos
- `data/fresh_76182.json` — catálogo fresco descifrado (con tokens de licencia)
- `unified-tv/` — app réplica (Gradle, CI en `.github/workflows/android.yml`):
  - `CatalogSource` — catálogo completo (remoto + asset)
  - `DirectSource` — streams directos públicos
  - `LiveSource` + `CoolxClient` — cliente del protocolo (EN PAUSA, ver abajo)
- APKs reempaquetados debuggable commiteados al repo (la cuota de artefactos de GitHub está llena)

## Estado y línea en pausa

**En pausa**: el cliente del portal dentro de UniPlay (CoolxClient/LiveSource). El gate del
operador existe para impedir clientes no autorizados del backend de un tercero; suplantarlo
cruza de análisis a evasión. El código quedó commiteado como referencia del protocolo verificado.

**Vías activas**:
1. UniPlay como reproductor genérico de fuentes legítimas del usuario (M3U/Xtream con suscripción, backend propio)
2. Catálogo público (canales tag=free con streams abiertos) → M3U
3. Informe de análisis documentado (este archivo + familia_xuper.md)

## Cómo usar el pipeline de captura

1. `gh workflow run repack -f apk_name=wiwyxuper.apk` (o el APK que sea) → el CI comitea el APK reempaquetado
2. `adb install repacked-*.apk` en el box (192.168.50.196:5555)
3. Lanzar la app; el logcat entrega: WXURL/WXCALL (peticiones), WXRESPX (respuestas, trozos de 3500),
   WXCENC/WXCKEY (crypto de salientes), WXDECIN/WXDECOUT (descifrados)
4. `run-as com.xuper.wiwyxuper` para shared_prefs (cache.config.xml: SN, userId, dataVersion) y
   `/sdcard/Android/data/com.xuper.wiwyxuper/files/Documents/` (cachés del catálogo, cifradas con b700bce0-...)
