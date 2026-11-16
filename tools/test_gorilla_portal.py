#!/usr/bin/env python3
"""Prueba: ¿es 146.71.124.186:80 el portal vivo del app? (getBaseTime + config/get con cuerpo cifrado correcto)"""
import sys
sys.path.insert(0, "/home/gumorenos/xuper-repo/tools")
import json, urllib.request
import portal_client

portal_client.PORTAL = "http://146.71.124.186"
for ep, payload in [("/api/portalCore/getBaseTime", {}),
                    ("/api/portalCore/config/get", {"groupName": "ApkConfig", "moduleName": "com.android.msandroid"})]:
    try:
        out = portal_client.call(ep, payload)
        print(ep, "->", json.dumps(out, ensure_ascii=False)[:400])
    except Exception as e:
        print(ep, "ERR:", e)
