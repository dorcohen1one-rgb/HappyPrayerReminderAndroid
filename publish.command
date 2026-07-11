#!/bin/zsh
set -e

cd "${0:A:h}"
./build-and-copy.command

git add -A
git commit -m "Launch living prayer ritual 4.0" || true
git push origin main

echo
echo "Version 4.0 is live on GitHub."
