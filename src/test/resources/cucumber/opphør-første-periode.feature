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
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                                                                             | Ugyldige begrunnelser |
      | 01.09.2023 | 31.08.2024 | OPPHØR             |                                | OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE, OPPHØR_IKKE_BOSATT_I_NORGE, OPPHØR_FLYTTET_FRA_NORGE, OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                                                                             | Eøsbegrunnelser | Fritekster |
      | 01.09.2023 | 31.08.2024 | OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE, OPPHØR_IKKE_BOSATT_I_NORGE, OPPHØR_FLYTTET_FRA_NORGE, OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE |                 |            |


    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2023 til 31.08.2024
      | Begrunnelse                          | Type     | Antall barn | Barnas fødselsdatoer | Gjelder søker | Beløp | Måned og år begrunnelsen gjelder for | Gjelder andre forelder | Antall timer barnehageplass |
      | OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   | 0                           |
      | OPPHØR_IKKE_BOSATT_I_NORGE           | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   | 0                           |
      | OPPHØR_FLYTTET_FRA_NORGE             | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   | 0                           |
      | OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   | 0                           |


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
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                 | Ugyldige begrunnelser |
      | 01.12.2023 | 30.11.2024 | OPPHØR             |                                | OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                 | Eøsbegrunnelser | Fritekster |
      | 01.12.2023 | 30.11.2024 | OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.12.2023 til 30.11.2024
      | Begrunnelse                          | Type     | Gjelder søker | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Gjelder andre forelder | Antall timer barnehageplass |
      | OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE | STANDARD | Ja            | 0           | desember 2023                        | 0     | false                  |                             |

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
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                                                                                                                                                          | Ugyldige begrunnelser |
      | 01.09.2023 | 31.08.2024 | OPPHØR             |                                | OPPHØR_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR, OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR, OPPHØR_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EOS_I_5_ÅR, OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_ELLER_EØS_I_5_AAR |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                                                                                                                                                          | Eøsbegrunnelser | Fritekster |
      | 01.09.2023 | 31.08.2024 | OPPHØR_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR, OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR, OPPHØR_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EOS_I_5_ÅR, OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_ELLER_EØS_I_5_AAR |                 |            |


    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2023 til 31.08.2024
      | Begrunnelse                                                   | Type     | Antall barn | Gjelder søker | Beløp | Måned og år begrunnelsen gjelder for | Gjelder andre forelder |
      | OPPHØR_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR                      | STANDARD | 0           | ja            | 0     | september 2023                       | false                  |
      | OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_I_5_ÅR            | STANDARD | 0           | ja            | 0     | september 2023                       | false                  |
      | OPPHØR_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EOS_I_5_ÅR              | STANDARD | 0           | ja            | 0     | september 2023                       | false                  |
      | OPPHØR_VURDERING_IKKE_MEDLEM_I_FOLKETRYGDEN_ELLER_EØS_I_5_AAR | STANDARD | 0           | ja            | 0     | september 2023                       | false                  |