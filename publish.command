#!/bin/zsh
set -e

cd "${0:A:h}"
./build-and-copy.command

git add -A
git commit -m "Give every sound world its own character" || true
git push origin main

echo
echo "Version 5.4 is live on GitHub."
