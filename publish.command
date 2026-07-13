#!/bin/zsh
set -e

cd "${0:A:h}"
./build-and-copy.command

git add -A
git commit -m "Improve listening flow and guided audio quality" || true
git push origin main

echo
echo "Version 5.3 is live on GitHub."
