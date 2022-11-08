PODNAVN=$(kubectl -n teamfamilie get pods -o name | grep familie-ks-sak | grep -v "frontend" |  sed "s/^.\{4\}//" | head -n 1);

echo $PODNAVN

PODVARIABLER="$(kubectl -n teamfamilie exec -c familie-ks-sak -it "$PODNAVN" -- env)"
AZURE_APP_CLIENT_ID="$(echo "$PODVARIABLER" | grep "AZURE_APP_CLIENT_ID" | tr -d '\r' )"
AZURE_APP_CLIENT_SECRET="$(echo "$PODVARIABLER" | grep "AZURE_APP_CLIENT_SECRET" | tr -d '\r' )";

if [ -z "$AZURE_APP_CLIENT_ID" ]
then
      echo "Klarte ikke hente variabler fra Nais. Er du logget på Naisdevice og gcloud?"
else
      printf "%s;%s" "$AZURE_APP_CLIENT_ID" "$AZURE_APP_CLIENT_SECRET" | pbcopy
fi