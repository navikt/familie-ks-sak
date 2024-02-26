﻿# Fiks feil med globbing (feil tolkning av * i url)
unsetopt glob

# Sjekk om vi er i riktig mappe. Gå til root om vi er i src/test/resources.
if [[ $PWD == *src/test/resources* ]]
then
  cd ../../..
fi

# URL for å hente begrunnelser og eøs-begrunnelser fra sanity
# Hentes ved å gjøre queryene fra no/nav/familie/ba/sak/integrasjoner/sanity/SanityQueries.kt i Vision
# URL til Vision: https://familie-brev.sanity.studio/ks-brev/vision
HENT_BEGRUNNELSER_URL="https://xsrv1mh6.api.sanity.io/v2022-03-07/data/query/ks-brev?query=*%5B_type%3D%3D%22ksBegrunnelse%22%5D"

SANITY_BEGRUNNELSER_MOCK_PATH="src/test/resources/cucumber/restSanityBegrunnelser"

# Hent begrunnelser fra sanity og lagre i fil
curl -XGET "$HENT_BEGRUNNELSER_URL" | jq '.result' -c > "$SANITY_BEGRUNNELSER_MOCK_PATH"
