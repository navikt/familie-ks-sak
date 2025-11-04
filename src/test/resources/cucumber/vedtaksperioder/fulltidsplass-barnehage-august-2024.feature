# language: no
# encoding: UTF-8

Egenskap: Fulltidsplass barnehage august 2024 med eksplisitt avslag

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 16.02.1986  |
      | 1            | 2       | BARN       | 09.05.2023  |
      | 2            | 1       | SØKER      | 16.02.1986  |
      | 2            | 2       | BARN       | 09.05.2023  |
      | 2            | 3       | BARN       | 20.08.2024  |

  Scenario: Endret utbetaling med fulltidsplass barnehage og eksplisitt avslag filtrerer vekk tidligere barn for begrunnelser og behandlingsresultat
    Og følgende dagens dato 31.10.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 16.02.1986 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 16.02.1991 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 28.09.1990 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 09.05.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 09.05.2023 | 07.08.2024 | OPPFYLT  | Nei                  |                      |                  | Ja                                    |              |
      | 2       | BARNETS_ALDER                |                  | 09.05.2024 | 09.05.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                           | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 16.02.1986 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 16.02.1991 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 28.09.1990 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 09.05.2023 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 09.05.2023 | 07.08.2024 | OPPFYLT      | Nei                  |                                                |                  | Ja                                    |              |
      | 2       | BARNETS_ALDER                |                  | 09.05.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                                                |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 01.08.2024 | 09.12.2024 | OPPFYLT      | Nei                  |                                                |                  | Nei                                   |              |

      | 3       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 28.09.1990 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               |                  | 20.08.2024 | 10.09.2025 | OPPFYLT      | Nei                  |                                                |                  | Nei                                   |              |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 20.08.2024 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BARNETS_ALDER                |                  | 20.08.2025 | 20.04.2026 | OPPFYLT      | Nei                  |                                                |                  | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               |                  | 11.09.2025 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_BRUKER_MELDER_FULLTIDSPLASS_I_BARNEHAGE |                  | Nei                                   | 40           |

    Og følgende endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak                                 | Prosent | Søknadstidspunkt | Er eksplisitt avslag |
      | 2       | 2            | 01.08.2024 | 31.08.2024 | FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 | 0       | 21.08.2025       | true                 |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.06.2024 | 31.07.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 2       | 01.08.2024 | 31.08.2024 | 0     | ORDINÆR_KONTANTSTØTTE | 0       | 7500 | 0                      |                          |
    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er AVSLÅTT på behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.09.2025 |          | AVSLAG             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                         | Ugyldige begrunnelser |
      | 01.09.2025 |          | AVSLAG             |                                | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 |                       |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2025 til -
      | Begrunnelse                                    | Type     | Barnas fødselsdatoer | Antall barn | Gjelder søker | Beløp | Måned og år begrunnelsen gjelder for | Antall timer barnehageplass | Måned og år før vedtaksperiode |
      | AVSLAG_BRUKER_MELDER_FULLTIDSPLASS_I_BARNEHAGE | STANDARD | 20.08.24             | 1           | Nei           | 0     | september 2025                       | 40                          | august 2025                    |