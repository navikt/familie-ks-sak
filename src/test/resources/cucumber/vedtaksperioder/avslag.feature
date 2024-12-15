# language: no
# encoding: UTF-8

Egenskap: Avslag perioder

  Scenario: Avslagperioder som starter samtidig som en andel slutter skal få fom flyttet med 1 mnd
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | LOVENDRING_2024  | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 13.07.1990  |
      | 1            | 2       | BARN       | 08.06.2023  |
      | 2            | 1       | SØKER      | 13.07.1990  |
      | 2            | 2       | BARN       | 08.06.2023  |

    Og følgende dagens dato 15.12.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 10.08.1996 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 10.08.2001 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 23.09.1991 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 08.06.2023 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 08.06.2023 | 04.08.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 08.06.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 01.08.2024 | 08.01.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 05.08.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 40           |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser             | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 10.08.1996 |            | OPPFYLT      | Nei                  |                                  | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 10.08.2001 |            | OPPFYLT      | Nei                  |                                  | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 23.09.1991 |            | OPPFYLT      | Nei                  |                                  | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 08.06.2023 |            | OPPFYLT      | Nei                  |                                  | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 08.06.2023 | 04.08.2024 | OPPFYLT      | Nei                  |                                  |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 08.06.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                                  |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 01.08.2024 | 08.01.2025 | OPPFYLT      | Nei                  |                                  |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 05.08.2024 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE |                  | Nei                                   | 40           |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.07.2024 | 31.08.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er AVSLÅTT på behandling 2


    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.09.2024 |          | AVSLAG             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser             | Ugyldige begrunnelser |
      | 01.09.2024 |          | AVSLAG             |                                | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser             | Eøsbegrunnelser | Fritekster |
      | 01.09.2024 |          | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2024 til -
      | Begrunnelse                      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Målform | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Gjelder andre forelder | Måned og år begrunnelsen gjelder for |
      | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE | STANDARD | Nei           | 08.06.23             | 1           | NB      | 0     |                  | 40                          | Nei                    | august 2024                          |

  Scenario: Avslagperioder som ikke starter samtidig som en andel slutter skal ha samme fom som vilkåret
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 28.12.2000  |
      | 1            | 2       | BARN       | 15.05.2023  |
      | 2            | 1       | SØKER      | 28.12.2000  |
      | 2            | 2       | BARN       | 15.05.2023  |

    Og følgende dagens dato 15.12.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 28.12.2000 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER |                  | 15.05.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.05.2023 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 15.05.2024 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 01.08.2024 | 15.12.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                  | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser     | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET               |                  | 28.12.2000 |            | OPPFYLT      | Nei                  |                          | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER |                  | 15.05.2023 |            | OPPFYLT      | Nei                  |                          | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET                          |                  | 15.05.2023 | 23.07.2024 | OPPFYLT      | Nei                  |                          | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                          |                  | 15.05.2023 | 20.08.2024 | OPPFYLT      | Nei                  |                          |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                           |                  | 15.05.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                          |                  | Nei                                   |              |
      | 2       | BOSATT_I_RIKET                          |                  | 24.07.2024 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_FLYTTET_FRA_NORGE | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNETS_ALDER                           |                  | 01.08.2024 | 15.12.2024 | OPPFYLT      | Nei                  |                          |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                          |                  | 21.08.2024 |            | OPPFYLT      | Nei                  |                          |                  | Nei                                   |              |

    Og følgende endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.06.2024 | 31.07.2024 | ETTERBETALING_3MND | 0       | 15.11.2024       |                             |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.06.2024 | 30.06.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er DELVIS_INNVILGET_ENDRET_OG_OPPHØRT på behandling 2


    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.06.2024 | 30.06.2024 | UTBETALING         |           |
      | 01.07.2024 |            | AVSLAG             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser     | Ugyldige begrunnelser |
      | 01.06.2024 | 30.06.2024 | UTBETALING         |                                |                          |                       |
      | 01.07.2024 |            | AVSLAG             |                                | AVSLAG_FLYTTET_FRA_NORGE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser     | Eøsbegrunnelser | Fritekster |
      | 01.06.2024 | 30.06.2024 |                          |                 |            |
      | 01.07.2024 |            | AVSLAG_FLYTTET_FRA_NORGE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.07.2024 til -
      | Begrunnelse              | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Gjelder andre forelder |
      | AVSLAG_FLYTTET_FRA_NORGE | STANDARD | Nei           | 15.05.23             | 1           | juli 2024                            | 0     |                  | 0                           |                        |