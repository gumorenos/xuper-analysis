# Plan de réplica — Xuper (todas las funcionalidades)

Basado en: `/home/gumorenos/analisis_Xuper_v4.34.5.md` (protocolo `coolx`/`portalCore` mapeado completo).

## 0. Dependencia crítica: las fuentes de datos

La réplica puede tener **todas las funcionalidades**, pero los datos vienen del operador y hay tres caminos posibles (elegir uno o combinar):

| Camino | Qué necesitas | Riesgo/Nota |
|---|---|---|
| **A. Cliente contra el portal original** | Cuenta/SN activo del operador + suplantar una versión vigente (v4.34.5 está desactivada: `portal200001`) | El operador puede cambiar el protocolo, bloquear por firma/dispositivo o cerrar; el contenido es del operador |
| **B. Captura de tráfico (mitm/Frida)** | Un dispositivo TV-box o emulador corriendo la app vigente + proxy (mitmproxy) o Frida | Necesario de todos modos para conocer la firma exacta de peticiones de la versión vigente y descifrar `config/get` |
| **C. Backend propio** (reimplementar `portalCore`) | Nada del operador; contenido propio (listas M3U/EPG XMLTV propios o licenciados) | Recomendado a largo plazo: sin dependencia del operador, legalmente más limpio |

**Recomendación:** B para mapear la firma/parámetros exactos → A para probar en vivo → C como destino final.

## 1. Arquitectura propuesta

- **App:** Kotlin, Android TV (Leanback + Compose-TV), minSdk 21, landscape/10-foot UI.
- **Player:** ExoPlayer (Media3) primario + ijkplayer como alternativa (cambio de player, como el original).
- **Red:** Retrofit/OkHttp + interceptor de firma (MD5+sal estilo `k2.b.c`) solo si apuntas al portal original; HTTPS con pinning opcional.
- **Módulos por feature flag** (uno por funcionalidad del original), para poder apagar lo que el backend no soporte.

## 2. Módulos (paridad 1:1 con el original)

| # | Módulo | Alcance | Dependencia de datos |
|---|---|---|---|
| 1 | **Base TV** | Home modular, navegación D-pad, tema 1920×1080, splash+guía | — |
| 2 | **Auth** | login/pass (MD5+sal), auto-login, registro+email, verificación, bind/unbind email-teléfono, **QR login**, cambio forzado de contraseña, gestión de dispositivos, heartbeat/logout | API portal (A) o JWT propio (C) |
| 3 | **Live TV + EPG** | canales, EPG diario (Room), panel de canal, favoritos, historial, retomar, reserva | A/C + EPG XMLTV en C |
| 4 | **Player UX** | HD/SD, PiP, cambio de player, aspect ratio, doblaje/audios, búsqueda por voz (SpeechRecognizer + beep) | — |
| 5 | **Deportes** | horarios de partidos, detalle, marcador en vivo, alineaciones, rankings, tarjetas | A (epg/v2-5) o proveedor propio |
| 6 | **VOD/Series** | estantes, categorías, tópicos, actores, búsqueda, filtros, recomendaciones, Kids | A (v3/*) o catálogo propio |
| 7 | **Descargas USB** | 1 descarga simultánea, límite de velocidad, pausa en reproducción, verificación de USB | — (streams de A/C) |
| 8 | **Control parental** | PIN adulto, bloqueo de canales/programas, bloqueo por horario, restricción de búsqueda | local (almacenado con Keystore) |
| 9 | **Monetización** | paquetes/suscripciones, órdenes, cupones, canje de códigos, invitar amigos, compra QR | A o Stripe/PayPal en C |
| 10 | **Notificaciones** | FCM, badge, notices propios, centro de eventos | A (notice API) o FCM propio |
| 11 | **Auto-update** | check upgrade, descarga APK, instalación (REQUEST_INSTALL_PACKAGES) | A (upgrade) o GitHub Releases en C |
| 12 | **Ads** | adserver report/get_content, bigbee, splash ads | A o admob en C |
| 13 | **Telemetría/anti-piratería** | uyumao cc_info/anti_logs, blacklists, Umeng-spy | ❌ **No replicar** (vigilancia del operador); sustituir por Crashlytics opcional |

## 3. Fases

1. **Fase 0 (datos):** captura de tráfico de la app vigente → documento `protocolo_vigente.md` con firma exacta, cifrado de `config/get` (3DES con PORTAL_KEY por-build), formato de respuestas y códigos de error (`portal*`).
2. **Fase 1:** Base TV + Auth + Live TV + player (MVP funcional contra A o C).
3. **Fase 2:** VOD/Kids/búsqueda + control parental + player UX completa.
4. **Fase 3:** deportes + descargas + notificaciones + auto-update.
5. **Fase 4:** monetización + ads + endurecimiento (pinning, Keystore, ofuscación R8 si se desea).

## 4. Notas legales y de seguridad

- Consumir el servicio de un operador IPTV no autorizado puede ser ilegal según jurisdicción; el camino C (backend propio + contenido licenciado/propio) es el limpio.
- No replicar: SDK antipiratería, geobloqueo remoto ni el mecanismo de "versión caducada" (apagado a distancia) — son las partes de control sobre el usuario.
- Nunca embeber claves simétricas fijas como el original (3DES/ECB con clave estática) — usar TLS + tokens de sesión.
