# language: no
# encoding: UTF-8

Egenskap: Fremtidig opphør - søker har meldt ifra om fremtidig barnehageplass

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Behandlingskategori |
      | 1            | 2        |                     | INNVILGET           | SØKNAD           | NASJONAL            |

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
      | Begrunnelse                            | Type     | Barnas fødselsdatoer | Antall barn | Beløp | Måned og år begrunnelsen gjelder for | Gjelder andre forelder |
      | OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS | STANDARD | 11.09.22             | 1           | 0     | august 2024                          | true                   |

  Scenario: Barnehageplass fra måneden før barnet fyller to år
    Og følgende dagens dato 06.02.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 23.08.1988 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |

      | 2       | BARNEHAGEPLASS                                         |                  | 11.09.2022 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                  | Ja                                    |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 11.09.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |
      | 2       | BARNETS_ALDER                                          |                  | 11.09.2023 | 11.09.2024 | OPPFYLT  | Nei                  |                      |                  |                                       |

    Og andeler er beregnet for behandling 1

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
      | Begrunnelse                            | Type     | Barnas fødselsdatoer | Antall barn | Målform | Beløp | Måned og år begrunnelsen gjelder for | Gjelder andre forelder |
      | OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS | STANDARD | 11.09.22             | 1           |         | 0     | august 2024                          | true                   |

  Scenario: Barnehageplass fra midten av en måned
    Og følgende dagens dato 06.02.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 23.08.1988 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |

      | 2       | BARNEHAGEPLASS                                         |                  | 11.09.2022 | 15.07.2024 | OPPFYLT  | Nei                  |                      |                  | Ja                                    |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 11.09.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |                                       |
      | 2       | BARNETS_ALDER                                          |                  | 11.09.2023 | 11.09.2024 | OPPFYLT  | Nei                  |                      |                  |                                       |

    Og andeler er beregnet for behandling 1

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
      | Begrunnelse                            | Type     | Barnas fødselsdatoer | Antall barn | Beløp | Måned og år begrunnelsen gjelder for | Gjelder andre forelder |
      | OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS | STANDARD | 11.09.22             | 1           | 0     | juli 2024                            | true                   |
