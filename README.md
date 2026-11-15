# Xuper — Ingeniería inversa y plan de réplica

Análisis estático y dinámico del APK **Xuper (com.android.mgstv) v4.34.5** (framework white-label *coolx*, empaquetado con Ijiami) y proyecto de réplica.

## Contenido

| Ruta | Descripción |
|---|---|
| `analisis_Xuper_v4.34.5.md` | Informe completo: identidad, funcionalidades, **fuentes de contenido descifradas** (dominios 3DES), API portalCore (60+ endpoints), 10 hallazgos de riesgo, addenda del sondeo en vivo |
| `plan_replica_Xuper.md` | Plan de réplica modular (12 módulos, 5 fases, caminos A/B/C para los datos) |
| `tools/decifrar_dominios.py` | Descifrador de los dominios embebidos: replica el decoder base64 propio del APK (caracteres inválidos → -1) + 3DES/ECB con la clave estática `*&@!6d5d-c483-4720-bb29-785b8f321c^%`. Uso: `python3 tools/decifrar_dominios.py` |
| `capture/dump_portal.py` | Addon de mitmproxy que registra cada request/respuesta de `portalCore`/`notice`/`config` a `captura.jsonl` |

## Hallazgos clave (TL;DR)

- APK empaquetado con **Ijiami**: 5 clases shell; el código real va cifrado en `assets/ijiami.dat`.
- Protocolo completo recuperado del framework hermano desempaquetado (v4.34.4-2, 9.005 clases).
- Dominios del operador **descifrados y verificados por DNS** (portal: `fuxok.nguvmqhpk.com`).
- `PORTAL_KEY` del manifiesto = material de clave 3DES por-build (no un URL cifrado).
- Portal activo pero **la v4.34.5 está desactivada** por el operador (`portal200001` "versión fuera de servicio").
- Firma de peticiones: MD5(param + `ntFT65w6itH!lHCPw7D=@qnsFC5adD28`).
- Anti-piratería: SDK `uyumao` → `yumao.puata.info` (`cc_info`, `anti_logs`, blacklists).

## Estado

- [x] Análisis estático + descifrado de fuentes de contenido
- [x] Sondeo en vivo del portal
- [ ] Captura de tráfico del TV-box con la versión vigente (pendiente de acceso adb)
- [ ] Réplica (Fases 1-4 del plan)

## Herramientas

- REA (JADX headless 0.7.1) · apktool · jadx CLI · mitmproxy 11 · adb
