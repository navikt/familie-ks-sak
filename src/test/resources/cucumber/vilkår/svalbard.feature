# language: no
# encoding: UTF-8

Egenskap: Svalbard

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |  | 1 |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 15.06.1994  |
      | 1            | 2       | BARN       | 23.02.2024  |

  Scenario: Ved splitt i vilkår grunnet bosatt på svalbard merkingen så skal ikke vedtaksperiodene splittes

    Og følgende dagens dato 01.09.2025

    Og følgende vilkårresultater for behandling 1

      | AktørId | Vilkår                                  | Utdypende vilkår   | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET                          |                    | 15.06.1994 | 02.07.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                              |                    | 15.06.1994 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | BOSATT_I_RIKET                          | BOSATT_PÅ_SVALBARD | 03.07.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER |                    | 23.02.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                          |                    | 23.02.2024 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOSATT_I_RIKET                          |                    | 23.02.2024 | 02.07.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNETS_ALDER                           |                    | 23.02.2025 | 23.10.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOSATT_I_RIKET                          | BOSATT_PÅ_SVALBARD | 03.07.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

    Og andeler er beregnet for behandling 1
    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.03.2025 | 30.09.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |

    Og når behandlingsresultatet er utledet for behandling 1

    Så forvent at behandlingsresultatet er INNVILGET_OG_OPPHØRT på behandling 1

    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.03.2025 | 30.09.2025 | UTBETALING         |           |
      | 01.10.2025 |            | OPPHØR             |           |