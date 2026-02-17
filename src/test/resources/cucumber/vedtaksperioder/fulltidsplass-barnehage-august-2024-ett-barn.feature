# language: no
# encoding: UTF-8

Egenskap: Fulltidsplass barnehage august 2024 med eksplisitt avslag - ett barn

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
      | 1            | 1       | SØKER      | 18.06.1991  |
      | 1            | 2       | BARN       | 06.05.2023  |
      | 2            | 1       | SØKER      | 18.06.1991  |
      | 2            | 2       | BARN       | 06.05.2023  |

    Og følgende søknadgrunnlag
      | BehandlingId | AktørId | Er inkludert i søknaden |
      | 1            | 2       | Ja                      |
      | 2            | 2       | Ja                      |

  Scenario: Avslag som følge av endret utbetaling skal inkludere barn det er søkt for
    Og følgende dagens dato 13.02.2026

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               | 18.06.1991 |            | OPPFYLT      | Nei                  | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   | 18.06.1996 |            | OPPFYLT      | Nei                  | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    | 11.06.1995 |            | OPPFYLT      | Nei                  | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER | 06.05.2023 |            | OPPFYLT      | Nei                  | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               | 06.05.2023 | 31.07.2024 | OPPFYLT      | Nei                  |                  | Ja                                    |              |
      | 2       | BARNETS_ALDER                | 06.05.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                | 01.08.2024 | 06.12.2024 | OPPFYLT      | Nei                  |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               | 01.08.2024 |            | IKKE_OPPFYLT | Nei                  |                  | Nei                                   | 40           |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               | 18.06.1991 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   | 18.06.1996 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    | 11.06.1995 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET | 06.05.2023 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNETS_ALDER                | 06.05.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               | 06.05.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                      |                  | Ja                                    |              |
      | 2       | BARNEHAGEPLASS               | 01.08.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 40           |
      | 2       | BARNETS_ALDER                | 01.08.2024 | 06.12.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og følgende endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak                                 | Prosent | Søknadstidspunkt | Er eksplisitt avslag |
      | 2       | 1            | 01.08.2024 | 31.08.2024 | FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 | 0       | 27.08.2024       | Ja                   |
      | 2       | 2            | 01.08.2024 | 31.08.2024 | FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 | 0       | 27.08.2024       | Ja                   |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp |
      | 2       | 01.06.2024 | 31.07.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |
      | 2       | 01.08.2024 | 31.08.2024 | 0     | ORDINÆR_KONTANTSTØTTE | 0       | 7500 | 0                      |
    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er AVSLÅTT på behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Vedtaksperiodetype |
      | 01.08.2024 | AVSLAG             |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | VedtaksperiodeType | Gyldige begrunnelser             |
      | 01.08.2024 | AVSLAG             | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Standardbegrunnelser             |
      | 01.08.2024 | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE |

    Så forvent følgende brevperioder for behandling 2
      | Brevperiodetype | Fra dato       | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | AVSLAG          | 1. august 2024 |          | 0     | 0                          |                     |                        |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.08.2024 til -
      | Begrunnelse                      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Gjelder andre forelder | Målform | Måned og år før vedtaksperiode |
      | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE | STANDARD |               | 06.05.23             | 1           | august 2024                          | 0     | 27.08.24         | 40                          |                        |         | juli 2024                      |