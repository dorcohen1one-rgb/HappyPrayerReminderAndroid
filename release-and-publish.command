#!/bin/zsh
set -euo pipefail

cd "${0:A:h}"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

VERSION="v1.9"
APK_SOURCE="app/build/outputs/apk/release/app-release.apk"
AAB_SOURCE="app/build/outputs/bundle/release/app-release.aab"
OUTPUT_DIR="apk-download"
APK_NAME="HappyPrayerReminder-v1.9.apk"
AAB_NAME="HappyPrayerReminder-v1.9.aab"
APK_PATH="$OUTPUT_DIR/$APK_NAME"
AAB_PATH="$OUTPUT_DIR/$AAB_NAME"
CHECKSUM_PATH="$APK_PATH.sha256"
AAB_CHECKSUM_PATH="$AAB_PATH.sha256"

echo "Building signed Happy Prayer Reminder $VERSION..."
./gradlew --no-daemon verifyReleaseSigning assembleRelease bundleRelease

mkdir -p "$OUTPUT_DIR"
cp "$APK_SOURCE" "$APK_PATH"
cp "$AAB_SOURCE" "$AAB_PATH"
shasum -a 256 "$APK_PATH" > "$CHECKSUM_PATH"
shasum -a 256 "$AAB_PATH" > "$AAB_CHECKSUM_PATH"

echo
echo "Signed APK: $PWD/$APK_PATH"
echo "SHA-256: $PWD/$CHECKSUM_PATH"
echo "Google Play AAB: $PWD/$AAB_PATH"
echo "AAB SHA-256: $PWD/$AAB_CHECKSUM_PATH"

git add -A
git commit -m "Release Happy Prayer Reminder $VERSION" || true
git push origin main

GH_BIN="$(command -v gh || true)"
if [[ -z "$GH_BIN" && -x "$HOME/.local/bin/gh" ]]; then
    GH_BIN="$HOME/.local/bin/gh"
fi

if [[ -z "$GH_BIN" ]]; then
    echo
    echo "APK was built, but GitHub CLI is not installed. Run ./install-github-cli.command, then '$HOME/.local/bin/gh auth login', and run this script again."
    exit 0
fi

if "$GH_BIN" release view "$VERSION" >/dev/null 2>&1; then
    "$GH_BIN" release upload "$VERSION" "$APK_PATH" "$CHECKSUM_PATH" "$AAB_PATH" "$AAB_CHECKSUM_PATH" --clobber
else
    "$GH_BIN" release create "$VERSION" "$APK_PATH" "$CHECKSUM_PATH" "$AAB_PATH" "$AAB_CHECKSUM_PATH" \
        --title "Happy Prayer Reminder $VERSION" \
        --notes "Signed Android release. The AAB is for Google Play; the APK is for direct installation. Verify downloads with the attached SHA-256 checksums."
fi

echo "GitHub Release $VERSION is live."
