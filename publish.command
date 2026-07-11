#!/bin/zsh
set -e

cd "${0:A:h}"
./build-and-copy.command

git add -A
git commit -m "Upgrade cosmic design to 2.3" || true
git push origin main

echo
echo "Version 2.3 is live on GitHub."
