# language: no
# encoding: UTF-8

Egenskap: Fremtidig opphør - søker har meldt ifra om fremtidig barnehageplass

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori |
      | 1            | 2        |                     | SØKNAD           | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 23.08.1988  |
      | 1            | 2       | BARN       | 11.09.2022  |

  Scenario: Barnehageplass fra måneden før barnet fyller to år
    Og følgende dagens dato 06.02.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 23.08.1988 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |

      | 2       | BARNEHAGEPLASS                                         |                  | 11.09.2022 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                  | Ja                                    |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 11.09.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |
      | 2       | BARNETS_ALDER                                          |                  | 11.09.2023 | 11.09.2024 | OPPFYLT  | Nei                  |                      |                  |                                       |

    Og andeler er beregnet for behandling 1

    Og når behandlingsresultatet er utledet for behandling 1

    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.10.2023 | 31.07.2024 | UTBETALING         |           |
      | 01.08.2024 |            | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 1
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                   | Ugyldige begrunnelser |
      | 01.10.2023 | 31.07.2024 | UTBETALING         |                                |                                        |                       |
      | 01.08.2024 |            | OPPHØR             |                                | OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS |                       |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.08.2024 til -
      | Begrunnelse                            | Type     | Barnas fødselsdatoer | Antall barn | Gjelder søker | Målform | Beløp | Søknadstidspunkt | Måned og år begrunnelsen gjelder for | Avtale tidspunkt delt bosted | Søkers rett til utvidet |
      | OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS | STANDARD | 11.09.22             | 1           |               |         | 0     |                  | august 2024                          |                              |                         |

  Scenario: Barnehageplass fra måneden før barnet fyller to år
    Og følgende dagens dato 06.02.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 23.08.1988 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |

      | 2       | BARNEHAGEPLASS                                         |                  | 11.09.2022 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                  | Ja                                    |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 11.09.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |
      | 2       | BARNETS_ALDER                                          |                  | 11.09.2023 | 11.09.2024 | OPPFYLT  | Nei                  |                      |                  |                                       |

    Og andeler er beregnet for behandling 1

    Og når behandlingsresultatet er utledet for behandling 1

    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.10.2023 | 31.07.2024 | UTBETALING         |           |
      | 01.08.2024 |            | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 1
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                   | Ugyldige begrunnelser |
      | 01.10.2023 | 31.07.2024 | UTBETALING         |                                |                                        |                       |
      | 01.08.2024 |            | OPPHØR             |                                | OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS |                       |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.08.2024 til -
      | Begrunnelse                            | Type     | Barnas fødselsdatoer | Antall barn | Gjelder søker | Målform | Beløp | Søknadstidspunkt | Måned og år begrunnelsen gjelder for | Avtale tidspunkt delt bosted | Søkers rett til utvidet |
      | OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS | STANDARD | 11.09.22             | 1           |               |         | 0     |                  | august 2024                          |                              |                         |

  Scenario: Barnehageplass fra midten av en måned
    Og følgende dagens dato 06.02.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 23.08.1988 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |

      | 2       | BARNEHAGEPLASS                                         |                  | 11.09.2022 | 15.07.2024 | OPPFYLT  | Nei                  |                      |                  | Ja                                    |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 11.09.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |
      | 2       | BARNETS_ALDER                                          |                  | 11.09.2023 | 11.09.2024 | OPPFYLT  | Nei                  |                      |                  |                                       |

    Og andeler er beregnet for behandling 1

    Og når behandlingsresultatet er utledet for behandling 1

    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.10.2023 | 30.06.2024 | UTBETALING         |           |
      | 01.07.2024 |            | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 1
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                   | Ugyldige begrunnelser |
      | 01.10.2023 | 30.06.2024 | UTBETALING         |                                |                                        |                       |
      | 01.07.2024 |            | OPPHØR             |                                | OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS |                       |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.07.2024 til -
      | Begrunnelse                            | Type     | Barnas fødselsdatoer | Antall barn | Gjelder søker | Målform | Beløp | Søknadstidspunkt | Måned og år begrunnelsen gjelder for | Avtale tidspunkt delt bosted | Søkers rett til utvidet |
      | OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS | STANDARD | 11.09.22             | 1           |               |         | 0     |                  | juli 2024                            |                              |                         |

  Scenario: Revurdering. Eneste endring er framtidig opphør framtidig opphør på barnehageplass. Skal gi behandlingsresultat opphør.
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | NYE_OPPLYSNINGER | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 15.11.1988  |
      | 1            | 2       | BARN       | 28.09.2022  |
      | 2            | 1       | SØKER      | 15.11.1988  |
      | 2            | 2       | BARN       | 28.09.2022  |

    Og følgende dagens dato 20.02.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass |
      | 1       | BOSATT_I_RIKET                |                  | 15.10.1988 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |
      | 1       | MEDLEMSKAP                    |                  | 15.11.1993 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER     |                  | 15.12.1980 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |
      | 2       | BOSATT_I_RIKET, BOR_MED_SØKER |                  | 28.09.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |
      | 2       | BARNEHAGEPLASS                |                  | 28.09.2022 |            | OPPFYLT  | Nei                  |                      |                  |                                       |
      | 2       | BARNETS_ALDER                 |                  | 28.09.2023 | 28.09.2024 | OPPFYLT  | Nei                  |                      |                  |                                       |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass |
      | 1       | BOSATT_I_RIKET                |                  | 15.10.1988 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |
      | 1       | MEDLEMSKAP                    |                  | 15.11.1993 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER     |                  | 15.12.1980 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |
      | 2       | BOSATT_I_RIKET, BOR_MED_SØKER |                  | 28.09.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |
      | 2       | BARNEHAGEPLASS                |                  | 28.09.2022 | 31.05.2024 | OPPFYLT  | Nei                  |                      |                  | Ja                                    |
      | 2       | BARNETS_ALDER                 |                  | 28.09.2023 | 28.09.2024 | OPPFYLT  | Nei                  |                      |                  |                                       |

    Og andeler er beregnet for behandling 1
    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.10.2023 | 31.05.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |

    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er OPPHØRT på behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.06.2024 |          | OPPHØR             |           |
