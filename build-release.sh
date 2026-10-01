#!/usr/bin/env bash
# Genera el AAB firmado de SoluPos para Google Play (macOS/Linux).
# Uso: ./build-release.sh   (pide la contraseña, no se guarda)
set -euo pipefail

cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export SOLUPOS_KEYSTORE_PATH="${SOLUPOS_KEYSTORE_PATH:-$HOME/.solupos/solupos-upload.jks}"
export SOLUPOS_KEY_ALIAS="${SOLUPOS_KEY_ALIAS:-solupos}"

[ -f "$SOLUPOS_KEYSTORE_PATH" ] || { echo "No se encontró el keystore en $SOLUPOS_KEYSTORE_PATH"; exit 1; }

read -r -s -p "Contraseña del keystore: " pass; echo
export SOLUPOS_KEYSTORE_PASSWORD="$pass" SOLUPOS_KEY_PASSWORD="$pass"

./gradlew bundleRelease --console=plain
echo "AAB: $PWD/app/build/outputs/bundle/release/app-release.aab"
