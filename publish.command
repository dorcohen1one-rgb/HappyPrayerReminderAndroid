#!/bin/zsh
set -e

cd "${0:A:h}"
./build-and-copy.command

git add -A
git commit -m "Reimagine the complete experience for 5.0" || true
git push origin main

echo
echo "Version 5.0 is live on GitHub."
