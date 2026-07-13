#!/bin/zsh
set -euo pipefail

architecture="amd64"
api_response="$(curl -fsSL https://api.github.com/repos/cli/cli/releases/latest)"
version="$(print -r -- "$api_response" | sed -n 's/.*"tag_name": "v\([^"]*\)".*/\1/p' | head -n 1)"
if [[ -z "$version" ]]; then
    echo "Could not determine the latest GitHub CLI version."
    exit 1
fi

temp_dir="$(mktemp -d)"
archive="gh_${version}_macOS_${architecture}.zip"
download_url="https://github.com/cli/cli/releases/download/v${version}/${archive}"

curl -fL "$download_url" -o "$temp_dir/$archive"
unzip -q "$temp_dir/$archive" -d "$temp_dir"
mkdir -p "$HOME/.local/bin"
cp "$temp_dir/gh_${version}_macOS_${architecture}/bin/gh" "$HOME/.local/bin/gh"
chmod +x "$HOME/.local/bin/gh"

echo "GitHub CLI installed:"
"$HOME/.local/bin/gh" --version
