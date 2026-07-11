#!/bin/zsh
set -e

cd "${0:A:h}"
./build-and-copy.command

git add -A
git commit -m "Launch generative soundscape experience 3.0" || true
git push origin main

echo
echo "Version 3.0 is live on GitHub."
