# language: no
# encoding: UTF-8

Egenskap: Behandlingsresultat ved nytt barn

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
      | 1            | 1       | SØKER      | 25.03.1994  |
      | 1            | 2       | BARN       | 14.05.2022  |
      | 2            | 1       | SØKER      | 25.03.1994  |
      | 2            | 2       | BARN       | 14.05.2022  |
      | 2            | 3       | BARN       | 28.06.2023  |

  Scenario: Skal bli AVSLÅTT dersom det er et nytt barn med avslag i behandling 2, og ingen endring for annet barn fra behandling 1
    Og følgende dagens dato 14.01.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               | 25.03.1994 |            | OPPFYLT      | Nei                  | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   | 25.03.1999 |            | OPPFYLT      | Nei                  | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    | 02.11.1999 |            | OPPFYLT      | Nei                  | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               | 14.05.2022 | 31.07.2023 | OPPFYLT      | Nei                  |                  | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER | 14.05.2022 |            | OPPFYLT      | Nei                  | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNETS_ALDER                | 14.05.2023 | 14.05.2024 | OPPFYLT      | Nei                  |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               | 01.08.2023 |            | IKKE_OPPFYLT | Nei                  |                  | Nei                                   | 50           |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                           | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               | 25.03.1994 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   | 25.03.1999 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    | 02.11.1999 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER | 14.05.2022 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               | 14.05.2022 | 31.07.2023 | OPPFYLT      | Nei                  |                                                |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                | 14.05.2023 | 14.05.2024 | OPPFYLT      | Nei                  |                                                |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               | 01.08.2023 |            | IKKE_OPPFYLT | Nei                  | AVSLAG_BRUKER_MELDER_FULLTIDSPLASS_I_BARNEHAGE |                  | Nei                                   | 50           |

      | 3       | MEDLEMSKAP_ANNEN_FORELDER    | 02.11.1999 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BOSATT_I_RIKET,BOR_MED_SØKER | 28.06.2023 |            | OPPFYLT      | Nei                  |                                                | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               | 28.06.2023 | 31.07.2024 | OPPFYLT      | Nei                  |                                                |                  | Nei                                   |              |
      | 3       | BARNETS_ALDER                | 28.06.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                                                |                  | Nei                                   |              |
      | 3       | BARNETS_ALDER                | 01.08.2024 | 28.01.2025 | OPPFYLT      | Nei                  |                                                |                  | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               | 01.08.2024 |            | IKKE_OPPFYLT | Nei                  | AVSLAG_BRUKER_MELDER_FULLTIDSPLASS_I_BARNEHAGE |                  | Nei                                   | 45           |

    Og følgende endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak                                 | Prosent | Søknadstidspunkt | Er eksplisitt avslag |
      | 3       | 2            | 01.07.2024 | 31.07.2024 | ETTERBETALING_3MND                    | 0       | 09.11.2024       | Ja                   |
      | 3       | 2            | 01.08.2024 | 31.08.2024 | FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 | 0       | 09.11.2024       | Ja                   |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp |
      | 2       | 01.06.2023 | 31.07.2023 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |
      | 3       | 01.07.2024 | 31.08.2024 | 0     | ORDINÆR_KONTANTSTØTTE | 0       | 7500 | 0                      |

    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er AVSLÅTT på behandling 2