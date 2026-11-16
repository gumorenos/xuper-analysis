#!/usr/bin/env python3
"""Prueba de registro de dispositivo: v3/snToken con la huella del box (solo lectura del servicio)."""
from portal_client import call
import json

bean = {
    "androidId": "", "wifiMac": "", "etheMac": "",
    "serialNumber": "ZTEATV45503609797",
    "diskInfo": "", "ramSize": "1.9 GB", "romSize": "4.1G",
    "gatewayMac": "d0:ea:11:a7:e8:16", "cpuId": "",
    "display": None, "brand": None, "verId": None, "cpuAbi": None,
    "hardware": None, "manufacturer": None, "fingerprint": None,
    "host": None, "device": None, "board": None, "tags": None,
}
print(json.dumps(call("/api/portalCore/v3/snToken", bean), ensure_ascii=False, indent=1)[:1500])
