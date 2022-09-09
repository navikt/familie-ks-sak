# familie-ks-sak

Saksbehandling for kontantstøtte

## Bygge lokalt

For å bygge tjenesten lokalt så kreves det at man har satt miljøvariabler først.

```
export GITHUB_USERNAME=`navident` (fra github)
export GITHUB_TOKEN=`github token` (fra github)
```

Deretter så kan denne kommandoen kjøres for å bygge


```
./gradlew clean build 
```
## Kjøring lokalt

For å kjøre opp appen lokalt kan en kjøre

* `DevLauncherPostgres`, som kjører opp med Spring-profilen `postgres` satt, og forventer en kjørende database.

Appen tilgjengeliggjøres da på `localhost:8089`. Se [Database](#database) for hvordan du setter opp databasen. For å
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

### Ktlint

* Vi bruker ktlint i dette prosjektet for å formatere kode.
* Du kan skru på automatisk reformattering av filer ved å installere en plugin som heter`Ktlint (unofficial)`
  fra `Preferences > Plugins > Marketplace`
* Gå til `Preferences > Tools > Actions on Save` og huk av så `Reformat code` og `Optimize imports` er markert.
* Gå til `Preferences > Tools > ktlint`og pass på at `Enable ktlint` og `Lint after Reformat` er huket av.

#### Manuel kjøring av ktlint

* Kjør `./gradlew ktlintFormat` i terminalen

## Produksjonssetting

Main-branchen blir automatisk bygget ved merge og deployet først til preprod og deretter til prod.

### Hastedeploy
Hvis vi trenger å deploye raskt til prod, har vi egne byggejobber for den biten, som trigges manuelt.

Den ene (krise-rett-i-prod) sjekker ut koden og bygger fra den.

Den andre (krise-eksisterende-image-rett-i-prod) lar deg deploye et tidligere bygd image. Det slår til for eksempel hvis du skal rulle tilbake til forrige versjon.
Denne tar som parameter taggen til imaget du vil deploye. Denne finner du under actions på GitHub, finn byggejobben du vil gå tilbake til, og kopier taggen derfra.

## Kontaktinformasjon

For NAV-interne kan henvendelser om applikasjonen rettes til #team-familie på slack. Ellers kan man opprette et issue
her på github.

## Tilgang til databasene i prod og preprod

Se https://github.com/navikt/familie/blob/master/doc/utvikling/gcp/gcp_kikke_i_databasen.md