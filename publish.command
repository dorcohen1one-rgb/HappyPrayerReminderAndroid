#!/bin/zsh
set -e

cd "${0:A:h}"
./build-and-copy.command

git add -A
git commit -m "Keep explicit alarm tests working in quiet mode" || true
git push origin main

echo
echo "Version 5.2 is live on GitHub."
