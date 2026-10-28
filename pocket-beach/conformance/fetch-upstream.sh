#!/usr/bin/env bash
# Fetch bsp-mcp-server and pscale-beach at the commits pinned in upstream.json,
# into conformance/upstream/, and install their own locked dependencies.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
for name in bsp-mcp-server pscale-beach; do
  repo=$(node -e "console.log(require('$here/upstream.json')['$name'].repo)")
  commit=$(node -e "console.log(require('$here/upstream.json')['$name'].commit)")
  dir="$here/upstream/$name"
  if [ "$(git -C "$dir" rev-parse HEAD 2>/dev/null || true)" != "$commit" ]; then
    rm -rf "$dir"
    mkdir -p "$dir"
    git -C "$dir" init -q
    git -C "$dir" remote add origin "$repo"
    git -C "$dir" fetch -q --depth 1 origin "$commit"
    git -C "$dir" checkout -q FETCH_HEAD
  fi
  (cd "$dir" && npm ci --no-audit --no-fund --ignore-scripts --loglevel=error)
done
echo "upstream ready: $(git -C "$here/upstream/bsp-mcp-server" rev-parse --short HEAD) $(git -C "$here/upstream/pscale-beach" rev-parse --short HEAD)"
