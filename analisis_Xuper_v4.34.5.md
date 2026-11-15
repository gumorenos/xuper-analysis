# Análisis estático — Xuper (com.android.mgstv) v4.34.5

**Archivo:** `/home/gumorenos/apk/Xuper (com.android.mgstv) [v.4.34.5].apk`
**SHA-256:** `1bf067d95ea5a2681ab06af70f921c2d1703070d291be4eb122e2413ff6ca7f2` (35.272.373 bytes)
**Fecha de análisis:** 2026-11-11 · Herramientas: REA (JADX headless 0.7.1, apktool), Craft Anything, jadx CLI

---

## 1. Identidad y protección

| Campo | Valor |
|---|---|
| Paquete | `com.android.mgstv` · versionName **4.34.5** (code 43405) |
| SDK | minSdk 19 · targetSdk 33 · compileSdk 34 |
| Marca | **Xuper** (launcher `com.interactive.brasiliptv.ui.activity.WelcomeActivity`) |
| Framework | **coolx** white-label TV (`org.coolx.tvframework`, `coolx_type=live`, PROGRAM_TYPE=26) |
| Orientación | Landscape, TV/leanback launcher, design 1920×1080 |
| Idiomas | ES / PT-BR / EN (+ spa, es, eng, en, pt, por en config de marca) |
| Firma | v1+v2, 1 certificado, SHA-256 `bd5ea57d9fc6f41677a0e97378e11d0aef2ba302c18716e88b9242fc3190857e` (declarada, no verificada criptográficamente) |
| Red | `cleartextTrafficPermitted="true"` global (`network_security_config`) |

**⚠️ Empaquetado:** el APK está protegido con **Ijiami (爱加密/SecNeo)**:

- `classes.dex` = solo 13,6 KB con 5 clases: `R` + shell `s.h.e.l.l.A/C/N/S` (Application y appComponentFactory del packer).
- Código real cifrado en `assets/ijiami.dat` (4,5 MB), `ijiami.ajm` (2,5 MB), `IJMDal.Data`, y `assets/ijm_lib/<abi>/libexec.so` (string `ijiami` dentro).
- Piezas anti-tamper: `af.bin`, `signed.bin`, `sign_verify.png`, imágenes "encrypted.png" señuelo.

Para el análisis del protocolo se usaron los **APK hermanos del mismo framework desempaquetados** (`Xuper Hydra - v4.34.4-2.apk` marca MAGIS y `XuperTv-N0F4C3-v4.34.7-NF-OFICIAL.apk`; 3 dex sin cifrar, ~17 MB, 9.005 clases). Toda la lógica de protocolo citada proviene de ahí (corrobación, no observación directa de v4.34.5).

---

## 2. Funcionalidades (observadas en manifiesto, recursos y código del framework)

### 2.1 Arranque
- Splash con video (`WelcomeActivity`, `VideoSplashListener`, 16 imágenes de carga), guía de primera vez (`GuidePageActivity`), mensaje "Illegal users" (`test1.html`) para usuarios no autorizados.
- Multi-proceso: `:gomediad` (GoMediaService, playback), `:channel` (ReportService, reportes), `:boost_multidex`.

### 2.2 Live TV
- Lista de canales + panel de canal (`ChannelPanelRootView`), EPG por día (`everyDayEpg`, `liveEpgDao` en Room/DBFlow).
- **Búsqueda por voz** (dos actividades dedicadas + `google.wav`).
- **Reserva de programas** (`LiveReserveRootView`, recordatorio).
- Configuración de reproducción: HD/SD, **PiP**, **cambio de player** (ijkplayer embebido), relación de aspecto, doblaje/audio ("Changing the dubbing").
- Favoritos, historial de visualización, retomar (fallback program / `getRallbackProgram`).

### 2.3 Deportes
- Horario de partidos, detalle, categorías y rankings (`MatchScheduleActivity`, `MatchDetailActivity`, `MatchCategoryActivity`, `MatchRankCategoryActivity`).
- API EPG deportiva: partidos de fútbol, alineaciones, marcador en vivo, eventos por equipo, próximos partidos; tarjetas rojas/amarillas.

### 2.4 VOD / Series
- Detalles con actores (`ActorDetailsActivity`), categorías, tópicos, filtros por género/contenido, búsqueda por nombre/contenido/BL, recomendaciones y estantes (`getShelveData`, `getRecommends`).
- **Modo Kids** (`KidsCategoryActivity`, fuente `amazingkids.otf`).
- **Control parental**: contraseña de adulto, bloqueo de canales y programas, **bloqueo por horario** (time lock), búsqueda con restricción (`RestrictLevelSearchActivity`), `updateRestrictedStatus`.
- **Descargas a USB**: una video a la vez, límite de velocidad, pausa durante reproducción, advertencia de no desconectar el pendrive; reproducible en otros dispositivos.

### 2.5 Cuenta y acceso
- Registro por email (dominios permitidos: gmail, hotmail, yahoo, outlook, live, uol.com.br, bol.com.br, globomail.com — mercado BR/LatAm).
- Login usuario/contraseña, auto-login, **login por QR** (token → allowLogin → getResult), verificación por código (email/SMS), bind/unbind de email y teléfono (códigos de país: 55, 598, 1, 81, …).
- **Cambio forzado de contraseña** (`ForcePasswordChangeActivity`), restablecimiento por email.
- **Gestión de dispositivos**: lista, límite, borrado remoto (`device-management/*`).
- Heartbeat de sesión (`v5/heartbeat`), logout.

### 2.6 Monetización (modelo de revendedor)
- Paquetes/suscripciones, compra y **historial de órdenes**, "Your service has expired, please contact your seller".
- **Compra escaneando QR** ("Scan QR code to buy").
- **Códigos de canje** de un solo uso (`v5/exchange`, "exchange code for special package can only be used once!").
- **Cupones** (`apkQueryCoupon`/`apkReceiveCoupon`), regalo de días por vincular email (`bindEmailGiftDays`).
- **Invitar amigos → VIP gratis** (`v2/getInviteCode`).
- Moneda "diamond" (servidor vacío en esta build).

### 2.7 Plataforma / infra
- **Auto-actualización**: servidor de upgrade + market interno; descarga APK e instala (`application/vnd.android.package-archive`, OkGo downloader); "APP will be out of date in 7 days"; `AUTO_UPDATE` meta por actividad.
- Anuncios: `adserver/report`, `adserver/v2/get_content`, sdk "bigbee".
- Notificaciones: Firebase Messaging, Umeng/ACCS/Agoo (Taobao), badges Huawei/Vivo, servidor de notices propio (`/notice/api/get_notice`).
- Telemetría: Firebase Analytics/Crashlytics, **AppMetrica (Yandex)**, Umeng (`libumeng-spy.so`), TNet/ACCS (Alibaba), Qiniu DNS con receiver de conectividad, fallback DoH `dns.google/resolve`.
- Config dinámica remota: `config/get`, `getBaseTime`, `PreInstall/auth`, `subs/terminal/metadata`.
- Tier gratuito: config en Firebase `mgs-free-e6046.appspot.com`.
- Soporte: `escuchamos-tv@outlook.com` (ES), `wuliang@gmail.com` (desarrollador).

---

## 3. Fuentes de contenido — extraídas y descifradas ✅

### 3.1 Dominios embebidos (descifrados con el propio algoritmo del APK)

Los dominios viajan cifrados (3DES/ECB/PKCS5) en `res/values/strings.xml` como hex. Cadena: hex → ASCII-base64 → bytes → 3DES-ECB con la clave estática embebida. **Todos descifrados y verificados por DNS (resuelven a Cloudflare) y padding PKCS5 válido (`0x05×5`):**

| Servicio | Dominio main | Dominio backup |
|---|---|---|
| **Portal (API principal)** | `fuxok.nguvmqhpk.com` | `mptec.dhkrxuzcy.com` |
| **EPG** | `vgwbm.uwfyobivh.com` | `robkd.ysrkwctjg.com`* |
| **Upgrade (auto-update)** | `jetsb.hgwtqrpmo.com` | `qkijv.rucvzndwa.com` |
| **Notice (notificaciones)** | `zxiws.tcgwhnvym.com` | `nxiqj.jgrqyxupl.com` |
| **Ads** | `yvhcn.hxjebagrv.com` | `zerif.qwcjdungi.com` |
| **Bigbee (ads)** | `eskna.ucpjdhivl.com` | `ernsm.prxmnvhcy.com` |
| **Scan QR login** | `dfcsq.divqohamz.com` | — |

\* `robkd.ysrkwctjg.com` (EPG backup). Vacíos en esta build: `market`, `diamond`, `dccore`, `datacollect`, `epg4b` (SN/data).

**Clave estática (embebida en `k2/b.java` → `fc/b.java`):** `*&@!6d5d-c483-4720-bb29-785b8f321c^%` — decodificada con el decoder base64 propio del APK (los caracteres inválidos `*&@!-^%` se mapean a `-1` y producen bytes deterministas).

**Segunda capa:** el meta-data del manifiesto `PORTAL_KEY` (hex `59507a…3d3d`) **no es un URL cifrado sino material de clave por-build**: se guarda como clave 3DES de runtime (`fc.b.f15947a`) que descifra la configuración cifrada que llega del servidor (`config/get`), y `key_sn_token` para el token de dispositivo (SN). Clave por defecto (fallback): `2b494e53756c664c2f44465245733572`.

### 3.2 API del portal (protocolo completo, de `{agreement}://{ip}/api/...`)

**Sesión/auth:** `v8/login`, `v8/active`, `terminalAuth`, `v3/snToken`, `v5/heartbeat`, `v5/loginOut`, `register`, `getVerifiCode`/`checkVerifiCode`/`v2/validateVerifyCode`, `pwdCheck`, `setPassword`, `v4/resetPwd`, `emailResetPassNotice`, `bindPhone`/`changeBindPhone`/`checkForceBind`, `v2/bindEmail`/`changeBindEmail`/`unBind`/`sendEmailVerifyCode`, `getEmailSuffix`, `getAreaCode`, `qr/token`, `qr/allowLogin`, `v5/qr/getResult`, `device/updateOrInsert`, `device-management/getDevice`/`deleteDevice`.

**Catálogo/live/VOD:** `getHome`, `v3/getShelveData`, `v3/getColumnContents`, `getRecommendColumnContents`, `v3/getColumnRecommendResource`, `v3/getRecommends`, `getNextColumns`, `v4/getItemData`, `v3/getProgram`, `getItemByPerson`, `getPerson`, `searchResourceOrPerson`, `v3/searchByName`/`searchByContent`, `v3/filterByContent`/`filterGenre`, `blSearchByName`/`blSearchByContent`, `liveSearchByName`, `getFavorite`/`v2/addFavorite`/`delFavorite`, `addSubscribe`/`delSubscribe`/`getSubscribe`, `getAllbackProgram`, `v2/getFree`, `v6/getLiveData`.

**Reproducción:** `v4/startPlayLive`, `v6/startPlayBTV`, `v10/startPlayVOD`, `v9/getAuthInfo`, `v14/getSlbInfo`, `v15/getSlbInfo` (balanceador SLB → aquí devuelve los URLs de stream, http/https/hls según operador), `subs/terminal/metadata`.

**EPG deportivo:** `epg/v2/getAllMatch`/`getLineUps`/`getMatchDetail`/`getScoreboard`/`getShelveMatch`/`getTeamEvent`, `epg/v3/getFootballMatch`, `epg/v4`+`v5/getNearestMatch`.

**Monetización:** `package/getOrderInfo`, `package/getPackageCustomization`, `getExchangeOrderInfo`, `v5/exchange`, `apkQueryCoupon`, `apkReceiveCoupon`, `bindEmailGiftDays`, `v2/getInviteCode`, `getPropertiesInfo`.

**Anuncios/config:** `adserver/report`, `adserver/v2/get_content`, `config/get`, `getBaseTime`, `api/PreInstall/auth`.

**Notificaciones:** `http://{host}/notice/api/get_notice?pkg={pkg}&v={ver}&sn={sn}&userId={userId}&language={lang}`.

**Anti-piratería (SDK uyumao):** `https://yumao.puata.info/cc_info` (3 rutas de envío) y `https://yumao.puata.info/anti_logs` — reporta fingerprints de dispositivo (`getAndroidId`, versión, marca), estado de blacklist de cuenta/IP/IPv6 (`account_blacklist`, `getIpv6BlackListTtl`, `ZmdManager`).

**Seguridad de peticiones:** firma combinada `k2.b.c(str)` = `md5(str + "ntFT65w6itH!lHCPw7D=@qnsFC5adD28")` + salteado adicional (`k2.b.i`); contraseñas viajan como MD5 combinado con la misma sal estática.

**Otros hosts:** `https://www.tvmagis.net/download` (portal de descargas de la marca MAGIS en el framework hermano; en la build Xuper el hub de descargas es vía QR/app market), `mgs-free-e6046.appspot.com` (config del tier gratis), endpoints Umeng (`errnewlog*.umeng.com`, `sss.umeng.com`) y Firebase (`app-measurement.com`).

**Nota importante:** los URLs de los streams de video NO van embebidos — se obtienen por sesión: `startPlayLive/BTV/VOD` + `getSlbInfo` responden con la URL del stream del operador (canales IPTV típicamente http/https HLS/TS). Sin captura de tráfico no hay más URLs de stream que extraer estáticamente.

---

## 4. Cosas sospechosas o cuestionables

| # | Hallazgo | Evidencia | Riesgo |
|---|---|---|---|
| 1 | **Packer Ijiami**: todo el código real cifrado; anti-análisis deliberado | `s.h.e.l.l.*`, `ijiami.dat`, string `ijiami` en `libexecmain.so` | Alto (transparencia) — típico de apps IPTV grises |
| 2 | **HTTP en claro globalmente permitido** | `network_security_config` | Medio: streams y API pueden viajar sin TLS |
| 3 | **Auto-instalación de APKs** fuera de store | `REQUEST_INSTALL_PACKAGES`, `u9/c.java` intent `package-archive`, strings de instalación | Medio-alto: vector de distribución de binarios del operador |
| 4 | **Telemetría múltiple y opaca** | Firebase, AppMetrica (Yandex), Umeng (`libumeng-spy.so` — el .so se llama así), ACCS/Agoo/TNet (Taobao), Qiniu | Medio: profiling comercial; ReportService en proceso propio `:channel` |
| 5 | **SDK antipiratería uyumao** que reporta fingerprints y blacklists (cuenta/IP/IPv6) a `yumao.puata.info` | `com.uyumao.*`, `cc_info`, `anti_logs`, `account_blacklist` | Medio: vigilancia del operador sobre tus dispositivos y hábitos |
| 6 | **Geobloqueo y listas negras del operador** | strings `account_blacklist` ("Due to policy limitation, your area cann't log on"), "This APP can't be used on this device", versión caducada | Bajo-técnico / alto-control: la app se apaga a distancia |
| 7 | **Criptografía débil y claves embebidas** | 3DES/ECB, clave estática en el código, sal MD5 en el APK, PORTAL_KEY reversible (demostrado: desciframos todo) | Alto para el operador, irrelevante para tu privacidad, pero permite replicar el cliente (y a cualquiera suplantar) |
| 8 | **Contraseñas hasheadas con MD5+sal estática** | `k2.b.c()` = MD5(pwd+sal) combinado | Medio: hash débil |
| 9 | **`setprop sys.subtitleService.disable false`** en Android 4.4 | `k2/b.java` `a()` | Bajo: cambia una propiedad del sistema (TV boxes antiguos); requiere privilegios |
| 10 | `allowBackup="true"`, `largeHeap`, `GET_TASKS`, `MOUNT_UNMOUNT_FILESYSTEMS` (legacy/inútil) | manifiesto | Bajo |

**No se observó** (dentro de los límites del análisis estático del código cifrado): acceso a contactos/SMS/ubicación/cámara/micrófono, root exploitation, ni exfiltración de datos personales más allá de fingerprints de dispositivo estándar. El empaquetado Ijiami limita esta afirmación: no es una auditoría del bytecode real de v4.34.5.

---

## 5. Limitaciones y desconocidos

1. **Código de v4.34.5 cifrado (Ijiami)** — para decompilar el bytecode real hay que desempaquetar en dispositivo/emulador (dump de dex en runtime con Frida) o confiar en el framework hermano desempaquetado (lo usado aquí).
2. **Contenido cifrado por sesión**: `config/get`, tokens SN, y respuestas de `startPlay*`/`getSlbInfo` llegan cifradas en runtime; sin captura de tráfico (mitm) no se conocen las URLs de stream reales ni la config actual del operador.
3. Los dominios descifrados son los **fallback embebidos de esta build**; el operador puede sobreescribirlos a distancia vía config (así es el diseño).
4. Firma declarada, no verificada criptográficamente.

---

## 6. Plan de réplica modular (a elegir por ti)

El protocolo está completamente mapeado; una réplica es viable como **cliente propio** que hable `/api/portalCore/*` contra el portal del operador (o contra un backend propio si lo que quieres es el producto completo). Módulos candidatos:

1. **Base TV** (navegación 10-foot, home modular, tema Xuper-like)
2. **Auth completa** (login/registro/auto-login/**QR**/verificaciones/cambio forzado de contraseña)
3. **Live TV + EPG + player** (ijkplayer o ExoPlayer, favoritos, historial, reserva, búsqueda por voz, PiP, cambio de player, doblaje)
4. **VOD/Series** (estantes, categorías, actores, tópicos, búsqueda, filtros, Kids)
5. **Deportes** (partidos, marcador, alineaciones, rankings)
6. **Descargas USB** (gestor con límite de velocidad y pausa)
7. **Control parental** (PIN, bloqueo por horario, restricción de búsqueda)
8. **Monetización** (paquetes, órdenes, cupones, canje, invitar amigos)
9. **Notificaciones push** (Firebase/Umeng)
10. **Auto-update** (upgrade server + instalación)
11. **Anti-piratería/telemetría** (uyumao, fingerprints, blacklists) — **no recomendado replicar**
12. **Backend propio** alternativo: reimplantar `portalCore` (auth JWT, catálogo, EPG, SLB de streams) para no depender del operador original

Camino técnico recomendado: Kotlin + TV (Leanback/Compose-TV), ExoPlayer/ijkplayer, Retrofit contra el catálogo de endpoints de la sección 3.2, clonando el esquema de firma (MD5+sal) solo si apuntas al mismo backend.

---

## 7. ADDENDA (2026-11-15) — Estado real de las fuentes de datos

Sondeo de solo lectura a los dominios descifrados:

| Prueba | Resultado |
|---|---|
| `https://fuxok.nguvmqhpk.com/` (portal) | 404 (Cloudflare, origen arriba) |
| `POST /api/portalCore/config/get` `{}` | **`returnCode 500` "系统内部错误"** — API viva, exige parámetros firmados |
| `POST /api/portalCore/v9/getAuthInfo` `{}` | **`portal200001` "版本已停止使用" (versión fuera de servicio)** |
| Notice server | 502 (origen caído o dominio de otra build) |

**Conclusiones:**
1. La infraestructura del operador está **activa** y el formato de respuesta es `{"returnCode","errorMessage","data"}`.
2. La identidad de **v4.34.5 está desactivada a distancia** por el operador (coincide con el string "That version is out of service" del APK). Una réplica debe presentarse con una identidad de versión vigente (spoof de versionCode/versionName + firma coherente) para ser aceptada.
3. El contenido real (canales, catálogo VOD, URLs de stream) requiere **sesión autenticada** (SN/token o cuenta de revendedor): no se puede obtener por análisis estático. Caminos posibles: (a) credenciales activas del usuario, (b) captura de tráfico (mitm) de un dispositivo funcionando con la versión vigente, (c) emulator + Frida.
