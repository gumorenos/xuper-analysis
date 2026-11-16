#!/bin/bash
# aapt2 x86 vía qemu-user con sysroot amd64 (Pi ARM64)
exec qemu-x86_64-static -L /usr/x86_64-linux-gnu /home/gumorenos/.gradle/caches/transforms-4/75c24ca921fe759e2533c6bd6c5318d1/transformed/aapt2-8.4.1-11315950-linux/aapt2 "$@"
