#!/usr/bin/env bash
# Regenerate the golden calls from bsp-mcp-server and pscale-beach at the
# commits pinned in upstream.json, and check the teaching shelf still matches.
#
#   conformance/generate.sh                    rewrite golden/golden.json
#   conformance/generate.sh <out.json>         write elsewhere (CI compares)
#   conformance/generate.sh <out.json> <n> <seed>   random cases only
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
export TZ=UTC
"$here/fetch-upstream.sh"
tsx="$here/upstream/bsp-mcp-server/node_modules/.bin/tsx"
if [ $# -ge 3 ]; then
  "$tsx" "$here/harness.mts" "$here/upstream" "$1" "$2" "$3" random-only
else
  "$tsx" "$here/harness.mts" "$here/upstream" "${1:-$here/golden/golden.json}"
fi
node "$here/export-shelf.mjs" "$here/upstream" "$here/../shelf" --check
