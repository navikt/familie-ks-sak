# Kafka Manager

[Kafka Manager](https://github.com/navikt/kafka-manager) er en applikasjon for å administrere Kafka-topics.
Du kan hente ut meldinger, se consumer-offsets og offset på siste melding, og sette consumer-offsets.

Appen er skrudd av som standard og deployes manuelt ved behov.

## Oppdatere versjonen

1. Åpne [Artifact Registry](https://console.cloud.google.com/artifacts/docker/nais-management-233d/europe-north1/poao/kafka-manager).
2. Finn nyeste publiserte image og kopier taggen.
3. Sett taggen som verdi for `kafkaManagerVersion` i [vars.yaml](vars.yaml). Dev og prod bruker samme fil.
4. Commit og push endringen til branchen du vil deploye fra.

## Deploye manuelt

Før du deployer, bør du [oppdatere til siste versjon](#oppdatere-versjonen) av Kafka Manager for å redusere risikoen for kjente sikkerhetshull.

1. Åpne **Actions** i GitHub og velg workflowen **Deploy familie-baks-kafka-manager**.
2. Trykk **Run workflow** og velg branchen med konfigurasjonen du vil deploye.
3. Huk av for `deploy_to_prod` hvis du også vil deploye til prod. Dev deployes alltid først.
4. Trykk **Run workflow** for å starte deployment.

Workflowen er definert i [deploy-kafka-manager.yaml](../../.github/workflows/deploy-kafka-manager.yaml).

## Etter bruk

Slett appen `familie-baks-kafka-manager` fra [Nais Console]https://console.nav.cloud.nais.io/team/teamfamilie/applications?filter=familie-baks-kafka-manager) i miljøene du deployet til, når du er ferdig med å bruke den.

## Miljøer

- Dev: https://familie-baks-kafka-manager.intern.dev.nav.no
- Prod: https://familie-baks-kafka-manager.intern.nav.no
