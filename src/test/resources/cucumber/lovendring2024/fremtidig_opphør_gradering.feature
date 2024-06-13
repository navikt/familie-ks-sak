# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - YwEMbPthsC

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 18.01.1989  |
      | 1            | 2       | BARN       | 12.05.2023  |

  Scenario: Plassholdertekst for scenario - bsbrkdTBsm
    Og følgende dagens dato 13.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 18.01.1989 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER,MEDLEMSKAP_ANNEN_FORELDER |                  | 12.06.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 16.04.2024 | 09.07.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 12.05.2024 | 12.05.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 10.08.2024 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 15           |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.06.2024 | 31.06.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 2       | 01.09.2024 | 31.04.2025 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 | 4500                   |                          |
    Og når behandlingsresultatet er utledet for behandling 1
    Så forvent at behandlingsresultatet er INNVILGET på behandling 1


    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.06.2024 | 30.06.2025 | UTBETALING         |           |
      | 01.09.2024 | 30.04.2025 | UTBETALING         |           |
      | 01.07.2024 | 31.08.2025 | OPPHØR             |           |
