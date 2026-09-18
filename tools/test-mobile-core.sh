#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/mobile-core
if javac -version 2>&1 | grep -q 'javac 1.8'; then
  options=(-source 8 -target 8)
else
  options=(--release 8)
fi
javac "${options[@]}" -d build/mobile-core client/src/com/voidclient/mobile/*.java client/test/com/voidclient/mobile/CoreRegression.java
java -cp build/mobile-core com.voidclient.mobile.CoreRegression
