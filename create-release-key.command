#!/bin/zsh
set -euo pipefail

PROJECT_DIR="${0:A:h}"
KEYSTORE_PATH="$HOME/.android/happy.jks"
KEYCHAIN_SERVICE="HappyPrayerRelease"
KEY_ALIAS="hp"
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
KEYTOOL="$JAVA_HOME/bin/keytool"

if [[ ! -x "$KEYTOOL" ]]; then
    echo "Android Studio Java was not found at: $KEYTOOL"
    exit 1
fi

password="$(security find-generic-password -a "$USER" -s "$KEYCHAIN_SERVICE" -w 2>/dev/null || true)"
if [[ -z "$password" ]]; then
    password="$(uuidgen | tr -d '-')"
    security add-generic-password -a "$USER" -s "$KEYCHAIN_SERVICE" -w "$password"
fi

mkdir -p "$HOME/.android"
if [[ ! -f "$KEYSTORE_PATH" ]]; then
    "$KEYTOOL" -genkeypair -v \
        -keystore "$KEYSTORE_PATH" \
        -storepass "$password" \
        -keypass "$password" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 3072 \
        -validity 10000 \
        -dname "CN=Dor"
fi

local_properties="$PROJECT_DIR/local.properties"
temp_file="$(mktemp)"
grep -v '^release\.' "$local_properties" 2>/dev/null > "$temp_file" || true
cat >> "$temp_file" <<EOF
release.storeFile=$KEYSTORE_PATH
release.storePassword=$password
release.keyAlias=$KEY_ALIAS
release.keyPassword=$password
EOF
mv "$temp_file" "$local_properties"

echo "Release signing is ready. The private key is at $KEYSTORE_PATH and its password is stored in macOS Keychain."
