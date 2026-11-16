# Análisis de la familia instalada en el box (2026-11-16)

Todos los "Xuper" y derivados son **builds del mismo framework white-label "coolx"** con distinta marca, dominios y capa custom. Además hay un outsider (MXL TV) con framework propio.

## Inventario (APKs instalados en el B866V2H)

| App | Paquete | Versión | Framework | Empaquetado | Fuentes de contenido |
|---|---|---|---|---|---|
| **WiwyXuper (TV)** | `com.xuper.wiwyxuper` | 4.34.4 (43404) | coolx | **Sin packer** (mod anti-telemetría) | Portal coolx `fuxok.nguvmqhpk.com` + custom DeporWiwy (OTA, ESPN, M3U hereisman, tvplusgratis, Firestore server-bundles, `*.tcpwe138stya.ru`) |
| **Xuper netxxus** | `com.xuper.netxxus` | 4.34.4 | coolx | Sin packer | Mismo portal coolx (repack de la Hydra v4.34.4-2 / MAGIS) |
| **XuperAmigOnly TV** | `com.xuper.amigonly.tv.new` | 2.5 | **Propio, mínimo** (1 dex, ExoPlayer) | No | **Streams DASH hardcodeados** (cdn-py.cvattv.com.ar: Telefuture/Unicanal/EWTN, etc.) |
| **WiwyXuper Móvil** | `com.xuper.wiwyxuper.mobile` | 6.5.85 (43665) | coolx (12 dex) | **Ijiami ligero** (ijiami.dat 225KB) + `primeview_access.dat` | Portal distinto: `dtgrd.txhnojlbu.com`, epg `cdtgcr.bcjoapser.com`, ads `skc2r.plracsimf.com`, notice `g4tc2.irlapchbd.com` (zonas wildcard diferentes, strings sin cifrar) |
| **MXL TV** | `tv.mxliptv.app` | 3.2.5-tvbox | **Propio** (7 dex, ExoPlayer/DASH) | No | Ecosistema propio (`mxl-apps.io`, scheme `mxltv://`), PayPal checkout, instabug, ironSource/Vungle/AdMob |

## Y los APK de la carpeta (~/apk)

| Archivo | Qué es |
|---|---|
| `Xuper (com.android.mgstv) [v.4.34.5].apk` | coolx empaquetado con Ijiami completo; versión **desactivada** por el operador (portal200001) |
| `Xuper Hydra - v4.34.4-2.apk` | coolx desempaquetado, marca **MAGIS** (`tvmagis.net/download`) |
| `XuperTv-N0F4C3-v4.34.7-NF-OFICIAL.apk` | coolx desempaquetado, v4.34.7 — el "NF" del usuario (misma cosa, build más nueva) |
| `base.apk` + `base (N).apk` | por identificar (5 dex) |
| `Famelack.apk`, `streamflix-v1.7.227-only-tv.apk` | otros reproductores IPTV no-coolx |

## Observaciones de la familia

1. **Dominios por marca/zona**: TV (`fuxok.nguvmqhpk.com`) vs Móvil (`dtgrd.txhnojlbu.com`) — el operador separa flotas por plataforma. Las zonas wildcard (`nguvmqhpk.com`, `txhnojlbu.com`, `bcjoapser.com`…) permiten rotar subdominios sin actualizar apps.
2. **El cifrado de dominios no es uniforme**: TV cifra con 3DES+clave estática; Móvil los trae en claro. El gate de versión (portal200001) opera por build.
3. **La capa custom (DeporWiwy) es la fuente real de "gratis"**: streams de terceros (tvplusgratis, barinaslive, golstream) y CDN directos (cvattv) — el portal coolx es solo el panel de pago/revendedor.
4. **XuperAmigOnly demuestra la variante minimalista**: ExoPlayer + lista de canales DASH hardcodeados. Es esencialmente "lo que un cliente unificado necesita" para la parte gratis.
5. **MXL TV es otro mundo**: panel propio + pagos PayPal + mucho ads. No conviene unificarlo; aporta solo ideas de UI/UX.

## Unificación en una sola app — propuesta

**Arquitectura: un solo APK (Kotlin, TV + móvil) con fuentes de datos intercambiables (pluggable sources):**

```
app/
 ├─ core/          (player ExoPlayer, EPG Room, auth, settings, parental)
 ├─ source-coolx/  (cliente portalCore: auth+QR+VOD+live+cupones — protocolo ya resuelto)
 ├─ source-m3u/    (listas M3U/XMLTV — la de hereisman o la que el usuario elija)
 ├─ source-direct/ (listas DASH/HLS fijas estilo AmigOnly)
 ├─ sports/        (ESPN API + marcador, estilo DeporWiwy)
 └─ ota/           (auto-update con manifest firmado, estilo wiwyxuper.pages.dev)
```

- **Modo A — panel operador:** usa `source-coolx` con la identidad de la versión vigente (Fase 0 pendiente: gate portal200001).
- **Modo B — independiente:** `source-m3u` + `source-direct` + EPG XMLTV; funciona sin el operador, replicando el valor real de AmigOnly/ExtraTV.
- Ambos modos conviven con un selector de fuente en Configuración.
