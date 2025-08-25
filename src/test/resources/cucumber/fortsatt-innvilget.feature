# language: no
# encoding: UTF-8

Egenskap: Fortsatt innvilget

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | NYE_OPPLYSNINGER | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 11.04.1987  |
      | 1            | 2       | BARN       | 07.05.2022  |
      | 2            | 1       | SØKER      | 11.04.1987  |
      | 2            | 2       | BARN       | 07.05.2022  |

  Scenario: Skal få riktig begrunnelse for fortsatt innvilget
    Og følgende dagens dato 15.01.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                   | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                |                  | 11.04.1987 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 07.05.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                            |                  | 07.05.2022 | 31.12.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNEHAGEPLASS                           |                  | 07.05.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BARNETS_ALDER                            |                  | 07.05.2023 | 07.05.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                            | DELT_BOSTED      | 01.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                   | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                |                  | 11.04.1987 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 07.05.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                            |                  | 07.05.2022 | 31.12.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNEHAGEPLASS                           |                  | 07.05.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BARNETS_ALDER                            |                  | 07.05.2023 | 07.05.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                            | DELT_BOSTED      | 01.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og andeler er beregnet for behandling 1
    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp |
      | 2       | 01.06.2023 | 31.01.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |
      | 2       | 01.02.2024 | 30.04.2024 | 3750  | ORDINÆR_KONTANTSTØTTE | 50      | 7500 | 3750                   |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp |
      | 2       | 01.06.2023 | 31.01.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |
      | 2       | 01.02.2024 | 30.04.2024 | 3750  | ORDINÆR_KONTANTSTØTTE | 50      | 7500 | 3750                   |

    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er FORTSATT_INNVILGET på behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato | Til dato | Vedtaksperiodetype |
      |          |          | FORTSATT_INNVILGET |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato | Til dato | VedtaksperiodeType | Gyldige begrunnelser                  |
      |          |          | FORTSATT_INNVILGET | FORTSATT_INNVILGET_BARN_BOR_MED_SOKER |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato | Til dato | Standardbegrunnelser                  |
      |          |          | FORTSATT_INNVILGET_BARN_BOR_MED_SOKER |

    Så forvent følgende brevperioder for behandling 2
      | Brevperiodetype    | Fra dato | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager |
      | FORTSATT_INNVILGET | Du får:  |          | 7 500 | 1                          | 07.05.22            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode - til -
      | Begrunnelse                           | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Målform | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Gjelder andre forelder |
      | FORTSATT_INNVILGET_BARN_BOR_MED_SOKER | STANDARD | Nei           | 07.05.22             | 1           | NB      | 7 500 |                  | 0                           | Ja                     |

  Scenario: Ved fortsatt innvilget skal den seneste barnehageplass vilkåret brukes når vi henter inn antall timer.
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |
    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | SØKNAD           | NASJONAL            | UTREDES           |
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 03.01.1998  |
      | 1            | 2       | BARN       | 04.06.2024  |
      | 1            | 3       | BARN       | 04.06.2024  |
      | 2            | 1       | SØKER      | 03.01.1998  |
      | 2            | 2       | BARN       | 04.06.2024  |
      | 2            | 3       | BARN       | 04.06.2024  |

    Og følgende dagens dato 21.08.2025
    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 03.01.1998 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 03.01.2003 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 03.06.1998 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 04.06.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 04.06.2024 | 31.07.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 04.06.2025 | 04.02.2026 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.08.2025 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 27           |
      | 3       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 03.06.1998 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 04.06.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               |                  | 04.06.2024 | 31.07.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 3       | BARNETS_ALDER                |                  | 04.06.2025 | 04.02.2026 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               |                  | 01.08.2025 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 27           |
    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 03.01.1998 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 03.01.2003 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 3       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 03.06.1998 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 04.06.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               |                  | 04.06.2024 | 31.07.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 3       | BARNETS_ALDER                |                  | 04.06.2025 | 04.02.2026 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               |                  | 01.08.2025 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 27           |
      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 03.06.1998 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 04.06.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 04.06.2024 | 31.07.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 04.06.2025 | 04.02.2026 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.08.2025 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 27           |
    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er FORTSATT_INNVILGET på behandling 2
    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato | Til dato | Vedtaksperiodetype | Kommentar |
      |          |          | FORTSATT_INNVILGET |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                        | Ugyldige begrunnelser |
      |          |          | FORTSATT_INNVILGET |                                | FORTSATT_INNVILGET_DELTIDSPLASS_I_BARNEHAGE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato | Til dato | Standardbegrunnelser                        | Eøsbegrunnelser | Fritekster |
      |          |          | FORTSATT_INNVILGET_DELTIDSPLASS_I_BARNEHAGE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode - til -
      | Begrunnelse                                 | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Gjelder andre forelder | Målform | Måned og år før vedtaksperiode |
      | FORTSATT_INNVILGET_DELTIDSPLASS_I_BARNEHAGE | STANDARD |               | 04.06.24 og 04.06.24 | 2           |                                      | 3 000 |                  | 27 og 27                    | Ja                     |         |                                |
