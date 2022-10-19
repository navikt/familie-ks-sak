# familie-ks-sak

Saksbehandling for kontantstøtte

## Bygge lokalt

For å bygge tjenesten lokalt så kreves det at man har satt miljøvariabler først.

```
export GITHUB_USERNAME=`navident` (fra github)
export GITHUB_TOKEN=`github token` (fra github)

** Windows brukere kan legge til de øvrige verdiene som miljøvariable i intellij
eller kan settes til en gradle.properties i ~/.gradle/
```

Deretter så kan denne kommandoen kjøres for å bygge

```
./gradlew clean build 
```

## Kjøring lokalt

For å kjøre opp appen lokalt kan en kjøre

* `DevLauncherPostgres`, som kjører opp med Spring-profilen `postgres` satt, og forventer en kjørende database.
* `DevLauncherPostgresPreprod`. Kjører mot intergrasjoner og pdl i preprod(ikke q1, men syntetisk). Har støtte for å
  kjøre mot andre miljøer, men da må mock manuelt kommenteres ut i DevLauncherPostgresPreprod. AZURE_APP_CLIENT_ID og
  AZURE_APP_CLIENT_SECRET må settes til familie-ks-sak sin azure client id og secret for å få tilgang til pdl og integrasjoner.
  Frontend må derfor bruke scope mot familie-ks-sak og ikke familie-ks-sak-lokal

Appen tilgjengeliggjøres da på `localhost:8083`. Se [Database](#database) for hvordan du setter opp databasen. For å
tillate kall fra frontend, se [Autentisering](#autentisering).

### Database

#### Embedded database

Bruker du `DevLauncherPostgres`, kan du kjøre opp en embedded database. Da må du sette `--dbcontainer`
under `Edit Configurations -> VM Options`

#### Database i egen container

Postgres-databasen kan settes opp slik:

```
docker run --name familie-ks-sak-postgres -e POSTGRES_PASSWORD=test -d -p 5432:5432 postgres
docker ps (finn container id)
docker exec -it <container_id> bash
psql -U postgres
CREATE DATABASE "familie-ks-sak";
```

### Autentisering

For å kalle applikasjonen fra fontend må du sette miljøvariablene AZURE_APP_CLIENT_ID og AZURE_APP_CLIENT_SECRET. Dette kan gjøres
under `Edit Configurations -> Environment Variables`. Miljøvariablene kan hentes fra `azuread-familie-ks-sak-lokal` i
dev-gcp-clusteret ved å gjøre følgende:

1. Logg på `gcloud`, typisk med kommandoen: `gcloud auth login`
2. Koble deg til dev-gcp-cluster'et: `kubectl config use-context dev-gcp`
3. Hent info:  
   `kubectl -n teamfamilie get secret azuread-familie-ks-sak-lokal -o json | jq '.data | map_values(@base64d)'`. Dersom du er på
   windows: `kubectl -n teamfamilie get secret azuread-familie-ks-sak-lokal -o json` og base64 decode verdiene "manuelt".

Kopier og sett verdiene til de lokale miljøvariablene `AZURE_APP_CLIENT_ID` og `AZURE_APP_CLIENT_SECRET`.

Se `.deploy/nais/azure-ad-app-lokal.yaml` dersom du ønsker å deploye `azuread-familie-ks-sak-lokal`

Til slutt skal miljøvariablene se slik ut:

DevLauncher/DevLauncherPostgres

* AZURE_APP_CLIENT_ID=(verdi fra `azuread-familie-ks-sak-lokal`)
* AZURE_APP_CLIENT_SECRET=(verdi fra `azuread-familie-ks-sak-lokal`)

DevLauncherPostgresPreprod:
krever at man henter azuread fra en pod til familie-ks-sak. Som rulleres oftere enn azuread-familie-ks-sak-lokal
`kubectl -n teamfamilie exec -c familie-ks-sak -it familie-ks-sak-byttmegmedpodid -- env | grep AZURE_APP_CLIENT`

* AZURE_APP_CLIENT_ID=(verdi fra `familie-ks-sak`)
* AZURE_APP_CLIENT_SECRET=(verdi fra `familie-ks-sak`)

### Ktlint

* Vi bruker ktlint i dette prosjektet for å formatere kode.
* Du kan skru på automatisk reformattering av filer ved å installere en plugin som heter`Ktlint (unofficial)`
  fra `Preferences/Settings > Plugins > Marketplace`
* Gå til `Preferences/Settings > Tools > Actions on Save` og huk av så `Reformat code` og `Optimize imports` er markert.
* Gå til `Preferences/Settings > Tools > ktlint`og pass på at `Enable ktlint` og `Lint after Reformat` er huket av.

#### Manuel kjøring av ktlint

* Kjør `./gradlew ktlintFormat` i terminalen

## Produksjonssetting

Main-branchen blir automatisk bygget ved merge og deployet først til preprod og deretter til prod.

### Hastedeploy

Hvis vi trenger å deploye raskt til prod, har vi egne byggejobber for den biten, som trigges manuelt.

Den ene (krise-rett-i-prod) sjekker ut koden og bygger fra den.

Den andre (krise-eksisterende-image-rett-i-prod) lar deg deploye et tidligere bygd image. Det slår til for eksempel hvis
du skal rulle tilbake til forrige versjon. Denne tar som parameter taggen til imaget du vil deploye. Denne finner du
under actions på GitHub, finn byggejobben du vil gå tilbake til, og kopier taggen derfra.

### Monitorering av kafka køer

Det er satt opp en tjeneste familie-ks-kafka-manager som er et verktøy for å monitorere
kafka meldinger i preprod og prod.

* Preprod: https://familie-ks-kafka-manager.dev.intern.nav.no
* Prod: https://familie-ks-kafka-manager.intern.nav.no

## Kontaktinformasjon

For NAV-interne kan henvendelser om applikasjonen rettes til #team-familie på slack. Ellers kan man opprette et issue
her på github.

## Tilgang til databasene i prod og preprod

Se https://github.com/navikt/familie/blob/master/doc/utvikling/gcp/gcp_kikke_i_databasen.md