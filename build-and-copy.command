#!/bin/zsh
set -e

cd "${0:A:h}"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

echo "Building Happy Prayer Reminder 2.3..."
./gradlew --no-daemon assembleDebug

mkdir -p apk-download
cp app/build/outputs/apk/debug/app-debug.apk apk-download/HappyPrayerReminder-2.3.apk
cp app/build/outputs/apk/debug/app-debug.apk apk-download/HappyPrayerReminder-debug.apk

echo
echo "Done! APK created at:"
echo "$PWD/apk-download/HappyPrayerReminder-2.3.apk"
open apk-download
