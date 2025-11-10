# language: no
# encoding: UTF-8

Egenskap: Sekundærland - opphør

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori |
      | 1            | 2        |                     | SØKNAD           | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 16.08.1988  |
      | 1            | 2       | BARN       | 15.09.2022  |

  Scenario: Skal få riktige begrunnelser ved opphør
    Og følgende dagens dato 18.01.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                  | Utdypende vilkår                    | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | MEDLEMSKAP                              |                                     | 16.08.1988 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET                          | OMFATTET_AV_NORSK_LOVGIVNING_UTLAND | 16.08.1988 | 15.11.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD                          |                                     | 16.08.1988 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | BOSATT_I_RIKET                          | BARN_BOR_I_EØS                      | 15.09.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER,MEDLEMSKAP_ANNEN_FORELDER |                                     | 15.09.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BARNEHAGEPLASS                          |                                     | 15.09.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BARNETS_ALDER                           |                                     | 15.09.2023 | 15.09.2024 | OPPFYLT  | Nei                  |                      |                  |

    Og følgende kompetanser for behandling 1
      | AktørId | Fra dato   | Til dato   | Resultat              | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.10.2023 | 31.10.2023 | NORGE_ER_SEKUNDÆRLAND | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |

    Og følgende utenlandske periodebeløp for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 01.10.2023 | 31.10.2023 | 1000  | PLN         | MÅNEDLIG  | PL              |

    Og følgende valutakurser for behandling 1
      | AktørId | Fra dato   | Til dato   | Valutakursdato | Valuta kode | Kurs         |
      | 2       | 01.10.2023 | 31.10.2023 | 2023-10-24     | PLN         | 2.6496751064 |

    Og andeler er beregnet for behandling 1

    Og når behandlingsresultatet er utledet for behandling 1

    Og vedtaksperioder er laget for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser                | Fritekster |
      | 01.11.2023 |          |                      | OPPHØR_SELVSTENDIG_RETT_OPPHØR |            |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato         | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | OPPHOR          | 1. november 2023 |          | 0     | 0                          |                     | Du                     |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.11.2023 til -
      | Begrunnelse                    | Type | Barnas fødselsdatoer | Antall barn | Målform | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland | Antall timer barnehageplass |
      | OPPHØR_SELVSTENDIG_RETT_OPPHØR | EØS  | 15.09.22             | 1           | NB      | arbeider         | inaktiv                   | Norge                 | Polen                          | Polen               | 0                           |

  Scenario: Ved opphør grunnet søkers vilkår, skal bare barn med relevante kompetanser trekkes inn
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | EØS                 | AVSLUTTET         |
      | 2            | 1        | 1                   | SØKNAD           | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 21.02.1992  |
      | 1            | 2       | BARN       | 10.01.2022  |
      | 2            | 1       | SØKER      | 21.02.1992  |
      | 2            | 2       | BARN       | 10.01.2022  |
      | 2            | 3       | BARN       | 13.02.2024  |

    Og følgende dagens dato 09.11.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                    | Utdypende vilkår             | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | MEDLEMSKAP                |                              | 21.02.1997 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 1       | LOVLIG_OPPHOLD            |                              | 01.09.2021 | 01.01.2024 | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 1       | BOSATT_I_RIKET            | OMFATTET_AV_NORSK_LOVGIVNING | 01.09.2021 | 01.01.2024 | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER |                              |            |            | IKKE_AKTUELT | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 2       | BARNEHAGEPLASS            |                              | 10.01.2022 |            | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOR_MED_SØKER             | BARN_BOR_I_EØS_MED_SØKER     | 10.01.2022 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 2       | BOSATT_I_RIKET            | BARN_BOR_I_EØS               | 10.01.2022 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 2       | BARNETS_ALDER             |                              | 10.01.2023 | 10.01.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                    | Utdypende vilkår             | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | MEDLEMSKAP                |                              | 21.02.1997 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 1       | BOSATT_I_RIKET            | OMFATTET_AV_NORSK_LOVGIVNING | 01.09.2021 | 02.07.2025 | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 1       | LOVLIG_OPPHOLD            |                              | 01.09.2021 | 02.07.2025 | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER |                              |            |            | IKKE_AKTUELT | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 2       | BOR_MED_SØKER             | BARN_BOR_I_EØS_MED_SØKER     | 10.01.2022 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 2       | BOSATT_I_RIKET            | BARN_BOR_I_EØS               | 10.01.2022 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 2       | BARNEHAGEPLASS            |                              | 10.01.2022 |            | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER             |                              | 10.01.2023 | 10.01.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

      | 3       | MEDLEMSKAP_ANNEN_FORELDER |                              |            |            | IKKE_AKTUELT | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 3       | BARNEHAGEPLASS            |                              | 13.02.2024 |            | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 3       | BOSATT_I_RIKET            | BARN_BOR_I_EØS               | 13.02.2024 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 3       | BOR_MED_SØKER             | BARN_BOR_I_EØS_MED_SØKER     | 13.02.2024 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 3       | BARNETS_ALDER             |                              | 13.02.2025 | 13.10.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og følgende endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt |
      | 2       | 1            | 01.02.2023 | 30.06.2023 | ETTERBETALING_3MND | 0       | 18.09.2023       |
      | 2       | 2            | 01.02.2023 | 30.06.2023 | ETTERBETALING_3MND | 0       | 18.09.2023       |

    Og følgende kompetanser for behandling 1
      | AktørId | Fra dato   | Til dato   | Resultat              | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.07.2023 | 31.12.2023 | NORGE_ER_SEKUNDÆRLAND | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Og følgende kompetanser for behandling 2
      | AktørId | Fra dato   | Til dato   | Resultat              | Søkers aktivitet                     | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.07.2023 | 31.12.2023 | NORGE_ER_SEKUNDÆRLAND | ARBEIDER                             | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 3       | 01.03.2025 | 30.06.2025 | NORGE_ER_SEKUNDÆRLAND | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | I_ARBEID                  | NO                    | PL                             | PL                  |

    Og følgende utenlandske periodebeløp for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 01.07.2023 | 31.12.2023 | 0     | PLN         | MÅNEDLIG  | PL              |

    Og følgende utenlandske periodebeløp for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 01.07.2023 | 31.12.2023 | 0     | PLN         | MÅNEDLIG  | PL              |
      | 3       | 01.03.2025 | 30.06.2025 | 500   | PLN         | MÅNEDLIG  | PL              |

    Og følgende valutakurser for behandling 1
      | AktørId | Fra dato   | Til dato   | Valutakursdato | Valuta kode | Kurs         |
      | 2       | 01.07.2023 | 31.12.2023 | 2023-12-29     | PLN         | 2.5902753773 |

    Og følgende valutakurser for behandling 2
      | AktørId | Fra dato   | Til dato   | Valutakursdato | Valuta kode | Kurs         |
      | 3       | 01.03.2025 | 30.06.2025 | 2025-02-03     | PLN         | 2.7721689741 |
      | 2       | 01.07.2023 | 31.12.2023 | 2023-12-29     | PLN         | 2.5902753773 |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.02.2023 | 30.06.2023 | 0     | ORDINÆR_KONTANTSTØTTE | 0       | 7500 | 0                      |                          |
      | 2       | 01.07.2023 | 31.12.2023 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   | 7500                     |
      | 3       | 01.03.2025 | 30.06.2025 | 6114  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   | 6114                     |
    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er INNVILGET_OG_OPPHØRT på behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.03.2025 | 30.06.2025 | UTBETALING         |           |
      | 01.07.2025 |            | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser     | Ugyldige begrunnelser |
      | 01.03.2025 | 30.06.2025 | UTBETALING         |                                | INNVILGET_IKKE_BARNEHAGE |                       |
      | 01.07.2025 |            | OPPHØR             |                                |                          |                       |
      | 01.07.2025 |            | OPPHØR             | EØS_FORORDNINGEN               | OPPHØR_EØS_STANDARD      |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser     | Eøsbegrunnelser     | Fritekster |
      | 01.03.2025 | 30.06.2025 | INNVILGET_IKKE_BARNEHAGE |                     |            |
      | 01.07.2025 |            |                          | OPPHØR_EØS_STANDARD |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.07.2025 til -
      | Begrunnelse         | Type | Gjelder søker | Barnas fødselsdatoer | Antall barn | Annen forelders aktivitet | Annen forelders aktivitetsland | Barnets bostedsland | Søkers aktivitet                     | Søkers aktivitetsland | Målform | Antall timer barnehageplass |
      | OPPHØR_EØS_STANDARD | EØS  |               | 13.02.24             | 1           | I_ARBEID                  | Polen                          | Polen               | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | Norge                 |         | 0                           |