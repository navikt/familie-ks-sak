#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../../.."

SANITY_API_VERSJON="v2021-06-07"
SANITY_QUERY_URL="https://xsrv1mh6.api.sanity.io/${SANITY_API_VERSJON}/data/query/ks-brev"

HENT_BEGRUNNELSER_QUERY='*[_type == "ksBegrunnelse" && apiNavn != null && navnISystem != null]'
SANITY_BEGRUNNELSER_MOCK_PATH="src/test/resources/cucumber/restSanityBegrunnelser.json"

hent_og_lagre() {
  local query="$1"
  local fil="$2"
  local tmp="${fil}.tmp"

  if curl -fsS --get "$SANITY_QUERY_URL" --data-urlencode "query=${query}" \
    | jq -S -e '.result | if type == "array" and length > 0 then sort_by(.apiNavn) else error("Tomt eller ugyldig svar fra Sanity") end' \
      > "$tmp"; then
    mv "$tmp" "$fil"
  else
    rm -f "$tmp"
    echo "Klarte ikke å hente begrunnelser til $fil" >&2
    exit 1
  fi
}

hent_og_lagre "$HENT_BEGRUNNELSER_QUERY" "$SANITY_BEGRUNNELSER_MOCK_PATH"
