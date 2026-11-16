# Notas de captura en vivo (box B866V2H, 2026-11-16)

Dispositivo: Huawei B866V2H (Android 14, armeabi-v7a, **sin root**, adb por red 192.168.50.196)
App capturada: com.xuper.wiwyxuper v4.34.4 (apkVer=43404), marca WiwyXuper, **APK desempaquetado** (sin Ijiami), modificado contra telemetría china (dominios Umeng/ACCS reescritos a `blocked.invalid`).

## Método
- `adb reverse tcp:8899 tcp:8899` + `settings put global http_proxy 127.0.0.1:8899` (el routing VLAN box→Raspberry estaba bloqueado; el túnel adb lo evita)
- mitmdump con `dump_portal.py` → `captura_box_2026-11-16.jsonl`
- **El portal portalCore NO pasa por el proxy del sistema** (usará OkHttp con TNet/DNS propio). Notice/ads/EPG-sports sí pasan (OkGo/OkHttp estándar).

## Confirmado en vivo
- `GET /notice/api/get_notice?pkg=com.xuper.wiwyxuper&v=49902&sn=cfe73e0cc6159eb1e97fc5ae62124029&userId=980151800&language=es` → servidores de notice **caídos (502 Cloudflare)**
- `POST /api/adserver/v2/get_content` (http claro, JSON plano): `{"ad_version":"1.0","apk_versioncode":"43476",...,"sn":"..."}` → responde `{"ad_positions":null,"expires_time":-1,"reporting_interval":60,...}`
- Sesión activa del box: `userId=980151800`, `sn=cfe73e0cc6159eb1e97fc5ae62124029`
- Tráfico del portal va DIRECTO: conexión establecida a `146.71.124.186:80` (PTR: static.gorillaservers.com) con ~14 KB en cola; también `64.31.43.179:33984` (long-connection) y Cloudflare 443 directo
- Dominios propios de la marca (capa custom `app.deporwiwy`): `wiwyxuper.pages.dev` (OTA JSON: última versión 4.35.56/43673 con SHA-256), ESPN (`site.api.espn.com`), Firestore (`rbapp-672ea`), M3U (`chatgpt.hereisman.net/playlist/58162`), `tvplusgratis.org/live*/core.php?canal=<canal>` (fallback deportivo hardcodeado), `barinaslive1.boldmss.com`, `golstream-iptv.pages.dev`, `*.tcpwe138stya.ru` (logos/datos)

## Protocolo portalCore (estado)
- Sobre cifrado resuelto: clave runtime = `b64decode(PORTAL_KEY)[:24]` (3DES/ECB/PKCS5); `config/get` etc. responden `{"returnCode","errorMessage","data"}` con `data` cifrado igual
- Headers de identidad (`zb/a.java`): `apk: com.android.msandroid`, `apkVer: 43404`, `spkgVer: <build>`
- Portal: `fuxok.nguvmqhpk.com` (respaldo `mptec.dhkrxuzcy.com`) — idéntico en todas las builds (strings cifrados compartidos)
- Estado actual de sondeos: `getBaseTime`/`getAuthInfo` → `portal200001 "versión fuera de servicio"`; pendiente replicar el gate de versión exacto (posible campo de cuerpo o registro previo del SN)
