# language: no
# encoding: UTF-8

Egenskap: Behandlingsresultat ved bruk av endret utbetaling andel

  Scenario: Opphørsperioder som starter og slutter samtidig som avslagsperioder skal bli filtrert ut.
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 21.03.1997  |
      | 1            | 2       | BARN       | 18.01.2023  |
      | 2            | 1       | SØKER      | 21.03.1997  |
      | 2            | 2       | BARN       | 18.01.2023  |


    Og følgende dagens dato 07.12.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 21.03.1997 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 18.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 18.01.2023 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 18.01.2024 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 01.08.2024 | 18.08.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 21.03.1997 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 18.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 18.01.2023 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 18.01.2024 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 01.08.2024 | 18.08.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og følgende endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak                                 | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted | Er eksplisitt avslag |
      | 2       | 2            | 01.08.2024 | 31.08.2024 | FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 | 0       | 04.12.2024       |                             | Ja                   |

    Og andeler er beregnet for behandling 1

    Og med følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.02.2024 | 31.07.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.02.2024 | 31.07.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 2       | 01.08.2024 | 31.08.2024 | 0     | ORDINÆR_KONTANTSTØTTE | 0       | 7500 | 0                      |                          |
    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er AVSLÅTT på behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.08.2024 |          | AVSLAG             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                         | Ugyldige begrunnelser |
      | 01.08.2024 |          | AVSLAG             |                                | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                         | Eøsbegrunnelser | Fritekster |
      | 01.08.2024 |          |                                              |                 |            |
      | 01.08.2024 |          | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 |                 |            |