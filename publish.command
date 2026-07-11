#!/bin/zsh
set -e

cd "${0:A:h}"
./build-and-copy.command

git add -A
git commit -m "Prevent unattended reminders from draining battery" || true
git push origin main

echo
echo "Version 5.1 is live on GitHub."
