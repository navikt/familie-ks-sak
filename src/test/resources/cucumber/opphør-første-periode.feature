# language: no
# encoding: UTF-8

Egenskap: Opphør første periode

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | OPPHØRT             | NYE_OPPLYSNINGER | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 14.07.1983  |
      | 1            | 2       | BARN       | 25.08.2022  |
      | 2            | 1       | SØKER      | 14.07.1983  |
      | 2            | 2       | BARN       | 25.08.2022  |

  Scenario: Når det er opphør første periode, men vi har vilkårresultat før den første, ønsker vi fortsatt å få opphørstekster for første periode
    Og følgende dagens dato 14.02.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET               |                  | 15.03.2010 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | MEDLEMSKAP                   |                  | 15.03.2015 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 01.12.2008 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 25.08.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNEHAGEPLASS               |                  | 25.08.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BARNETS_ALDER                |                  | 25.08.2023 | 25.08.2024 | OPPFYLT  | Nei                  |                      |                  |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                    | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET            |                  | 15.03.2010 | 01.09.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | MEDLEMSKAP                |                  | 15.03.2015 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER |                  | 01.12.2008 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNEHAGEPLASS            |                  | 25.08.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET            |                  | 25.08.2022 | 01.09.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER             |                  | 25.08.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNETS_ALDER             |                  | 25.08.2023 | 25.08.2024 | OPPFYLT  | Nei                  |                      |                  |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                                                                             | Ugyldige begrunnelser |
      | 01.09.2023 |          | OPPHØR             |                                | OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE, OPPHØR_IKKE_BOSATT_I_NORGE, OPPHØR_FLYTTET_FRA_NORGE, OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                                                                                                             | Eøsbegrunnelser | Fritekster |
      | 01.09.2023 |          | OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE, OPPHØR_IKKE_BOSATT_I_NORGE, OPPHØR_FLYTTET_FRA_NORGE, OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE |                 |            |


    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2023 til -
      | Begrunnelse                          | Type     | Antall barn | Barnas fødselsdatoer | Gjelder søker | Beløp | Måned og år begrunnelsen gjelder for | Gjelder andre forelder | Antall timer barnehageplass | Måned og år før vedtaksperiode |
      | OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   | 0                           | august 2023                    |
      | OPPHØR_IKKE_BOSATT_I_NORGE           | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   | 0                           | august 2023                    |
      | OPPHØR_FLYTTET_FRA_NORGE             | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   | 0                           | august 2023                    |
      | OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   | 0                           | august 2023                    |


  Scenario: Når det er opphør første periode men ikke på barn ønsker vi at barnet ikke flettes inn

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 14.01.2004  |
      | 1            | 2       | BARN       | 15.11.2022  |
      | 2            | 1       | SØKER      | 14.01.2004  |
      | 2            | 2       | BARN       | 15.11.2022  |

    Og følgende dagens dato 13.03.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass |
      | 1       | BOSATT_I_RIKET               |                  | 14.01.2004 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |
      | 1       | MEDLEMSKAP                   |                  | 14.01.2009 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  |            |            | IKKE_AKTUELT | Nei                  |                      | NASJONALE_REGLER | Nei                                   |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 15.11.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |
      | 2       | BARNEHAGEPLASS               |                  | 15.11.2022 |            | OPPFYLT      | Nei                  |                      |                  | Nei                                   |
      | 2       | BARNETS_ALDER                |                  | 15.11.2023 | 15.11.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass |
      | 1       | BOSATT_I_RIKET               |                  | 14.01.2004 | 15.12.2023 | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |
      | 1       | MEDLEMSKAP                   |                  | 14.01.2009 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  |            |            | IKKE_AKTUELT | Nei                  |                      | NASJONALE_REGLER | Nei                                   |
      | 2       | BARNEHAGEPLASS               |                  | 15.11.2022 |            | OPPFYLT      | Nei                  |                      |                  | Nei                                   |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 15.11.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |
      | 2       | BARNETS_ALDER                |                  | 15.11.2023 | 15.11.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |

    Og andeler er beregnet for behandling 1
    Og andeler er beregnet for behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                 | Ugyldige begrunnelser |
      | 01.12.2023 |          | OPPHØR             |                                | OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                 | Eøsbegrunnelser | Fritekster |
      | 01.12.2023 |          | OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.12.2023 til -
      | Begrunnelse                          | Type     | Gjelder søker | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Gjelder andre forelder | Antall timer barnehageplass | Måned og år før vedtaksperiode |
      | OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE | STANDARD | Ja            | 0           | desember 2023                        | 0     | false                  |                             | november 2023                  |

  Scenario: Når det er ikke oppfylt fra første periode på revurdering ønsker vi å få opp riktige begrunnelser
    Og følgende dagens dato 14.02.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET               |                  | 15.03.2010 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | MEDLEMSKAP                   |                  | 15.10.2018 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 01.12.2008 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 25.08.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNEHAGEPLASS               |                  | 25.08.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BARNETS_ALDER                |                  | 25.08.2023 | 25.08.2024 | OPPFYLT  | Nei                  |                      |                  |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET               |                  | 15.03.2010 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 1       | MEDLEMSKAP                   |                  | 15.09.2023 |            | IKKE_OPPFYLT | Nei                  |                      | NASJONALE_REGLER |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 01.12.2008 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 25.08.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNEHAGEPLASS               |                  | 25.08.2022 |            | OPPFYLT      | Nei                  |                      |                  |
      | 2       | BARNETS_ALDER                |                  | 25.08.2023 | 25.08.2024 | OPPFYLT      | Nei                  |                      |                  |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                                                                                                                | Ugyldige begrunnelser |
      | 01.09.2023 |          | OPPHØR             |                                | OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR, OPPHØR_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EOS_I_5_ÅR, OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_ELLER_EØS_I_5_AAR |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                                                                                                                                                | Eøsbegrunnelser | Fritekster |
      | 01.09.2023 |          | OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR, OPPHØR_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EOS_I_5_ÅR, OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_ELLER_EØS_I_5_AAR |                 |            |


    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2023 til -
      | Begrunnelse                                                   | Type     | Antall barn | Gjelder søker | Beløp | Måned og år begrunnelsen gjelder for | Gjelder andre forelder | Måned og år før vedtaksperiode |
      | OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR            | STANDARD | 0           | ja            | 0     | september 2023                       | false                  | august 2023                    |
      | OPPHØR_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EOS_I_5_ÅR              | STANDARD | 0           | ja            | 0     | september 2023                       | false                  | august 2023                    |
      | OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_ELLER_EØS_I_5_AAR | STANDARD | 0           | ja            | 0     | september 2023                       | false                  | august 2023                    |

  Scenario: Når et vilkår ikke er oppfylt måneden før vedtaksperioden ønsker vi å få begrunnelser knyttet til vilkåret
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 16.04.1997  |
      | 1            | 2       | BARN       | 02.05.2023  |
      | 2            | 1       | SØKER      | 16.04.1997  |
      | 2            | 2       | BARN       | 02.05.2023  |

    Og følgende dagens dato 25.09.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 04.11.2021 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 31.05.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 02.05.2023 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 02.05.2023 | 31.07.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 02.05.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 31.05.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.08.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 40           |
      | 2       | BARNETS_ALDER                |                  | 01.08.2024 | 02.12.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 04.11.2021 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 31.05.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 02.05.2023 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 31.05.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 02.05.2023 | 05.05.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 06.05.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 40           |
      | 2       | BARNETS_ALDER                |                  | 02.05.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 01.08.2024 | 02.12.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.06.2024 |          | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                           | Ugyldige begrunnelser |
      | 01.06.2024 |          | OPPHØR             |                                | OPPHØR_KOMMUNEN_MELDT_FULLTIDSPLASS_I_BARNEHAGE_FØRSTE_PERIODE |                       |


  Scenario: Ved opphør fra start langt tilbake før vedtaksperioden så skal vi få opp begrunnelser knyttet til utgjørende vilkår
    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | NYE_OPPLYSNINGER | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 15.05.1994  |
      | 1            | 2       | BARN       | 13.06.2022  |
      | 2            | 1       | SØKER      | 15.05.1994  |
      | 2            | 2       | BARN       | 13.06.2022  |

    Og følgende dagens dato 08.10.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | MEDLEMSKAP                   |                  | 13.06.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | BOSATT_I_RIKET               |                  | 23.06.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 13.06.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 13.06.2022 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 13.06.2023 | 13.06.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 23.06.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | MEDLEMSKAP                   |                  | 13.06.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | BOSATT_I_RIKET               |                  | 23.06.2023 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 13.06.2022 |            | IKKE_OPPFYLT | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 13.06.2022 |            | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 13.06.2023 | 13.06.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 23.06.2023 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

    Og følgende endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.07.2023 | 30.11.2023 | ETTERBETALING_3MND | 0       | 24.03.2024       |                             |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Og når behandlingsresultatet er utledet for behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.07.2023 |          | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                     | Ugyldige begrunnelser |
      | 01.07.2023 |          | OPPHØR             |                                | OPPHØR_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR |                       |

  Scenario: Ved opphør i første periode ønsker vi å bruke det seneste vilkåret ikke oppfylte vilkåret når vi referer til måneden begrunnelsene gjelder
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | NYE_OPPLYSNINGER | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 26.12.1986  |
      | 1            | 2       | BARN       | 04.01.2024  |
      | 2            | 1       | SØKER      | 26.12.1986  |
      | 2            | 2       | BARN       | 04.01.2024  |

    Og følgende dagens dato 18.05.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 26.12.1986 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 26.12.1991 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 24.03.1999 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 04.01.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 04.01.2024 | 31.08.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.09.2024 | 31.03.2025 | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 45           |
      | 2       | BARNETS_ALDER                |                  | 04.01.2025 | 04.09.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.04.2025 |            | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 26.12.1986 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 26.12.1991 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 24.03.1999 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 04.01.2024 | 31.08.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 04.01.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.09.2024 | 31.03.2025 | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 45           |
      | 2       | BARNETS_ALDER                |                  | 04.01.2025 | 04.09.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.04.2025 | 04.05.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 05.05.2025 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 45           |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er OPPHØRT på behandling 2


    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.05.2025 |          | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                           | Ugyldige begrunnelser |
      | 01.05.2025 |          | OPPHØR             |                                | OPPHØR_BRUKER_MELDER_FULLTIDSPLASS_I_BARNEHAGEN_FØRSTE_PERIODE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                                           | Eøsbegrunnelser | Fritekster |
      | 01.05.2025 |          | OPPHØR_BRUKER_MELDER_FULLTIDSPLASS_I_BARNEHAGEN_FØRSTE_PERIODE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.05.2025 til -
      | Begrunnelse                                                    | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Gjelder andre forelder | Målform | Måned og år før vedtaksperiode |
      | OPPHØR_BRUKER_MELDER_FULLTIDSPLASS_I_BARNEHAGEN_FØRSTE_PERIODE | STANDARD |               | 04.01.24             | 1           | mai 2025                             | 0     |                  | 45                          | Ja                     |         | april 2025                     |