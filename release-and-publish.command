#!/bin/zsh
set -euo pipefail

cd "${0:A:h}"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

VERSION="v1.0"
APK_SOURCE="app/build/outputs/apk/release/app-release.apk"
OUTPUT_DIR="apk-download"
APK_NAME="HappyPrayerReminder-v1.0.apk"
APK_PATH="$OUTPUT_DIR/$APK_NAME"
CHECKSUM_PATH="$APK_PATH.sha256"

echo "Building signed Happy Prayer Reminder $VERSION..."
./gradlew --no-daemon verifyReleaseSigning assembleRelease

mkdir -p "$OUTPUT_DIR"
cp "$APK_SOURCE" "$APK_PATH"
shasum -a 256 "$APK_PATH" > "$CHECKSUM_PATH"

echo
echo "Signed APK: $PWD/$APK_PATH"
echo "SHA-256: $PWD/$CHECKSUM_PATH"

git add -A
git commit -m "Release Happy Prayer Reminder $VERSION" || true
git push origin main

if ! command -v gh >/dev/null 2>&1; then
    echo
    echo "APK was built, but GitHub CLI is not installed. Install gh, run 'gh auth login', then run this script again to publish the Release."
    exit 0
fi

if gh release view "$VERSION" >/dev/null 2>&1; then
    gh release upload "$VERSION" "$APK_PATH" "$CHECKSUM_PATH" --clobber
else
    gh release create "$VERSION" "$APK_PATH" "$CHECKSUM_PATH" \
        --title "Happy Prayer Reminder $VERSION" \
        --notes "Signed Android release. Verify the APK with the attached SHA-256 checksum before installation."
fi

echo "GitHub Release $VERSION is live."
