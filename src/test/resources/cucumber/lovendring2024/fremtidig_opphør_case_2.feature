# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - FF8fuFKtY0

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | FATTER_VEDTAK     |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 18.05.1989  |
      | 1            | 2       | BARN       | 09.03.2023  |

  Scenario: Plassholdertekst for scenario - i12yUzoUYt
    Og følgende dagens dato 13.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                              |                  | 18.05.1989 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOR_MED_SØKER,MEDLEMSKAP_ANNEN_FORELDER,BOSATT_I_RIKET |                  | 09.03.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 16.04.2023 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                  | Ja                                    |              |
      | 2       | BARNETS_ALDER                                          |                  | 09.03.2024 | 09.03.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.04.2024 | 31.07.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
    Og når behandlingsresultatet er utledet for behandling 1
    Så forvent at behandlingsresultatet er INNVILGET_OG_OPPHØRT på behandling 1


    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.04.2024 | 31.08.2024 | UTBETALING         |           |
      | 01.09.2024 |            | OPPHØR             |           |