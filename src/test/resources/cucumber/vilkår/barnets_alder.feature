# language: no
# encoding: UTF-8

Egenskap: Barnets alder

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            |


  Scenario: Man skal ha rett til kontantstøtte samme måned som barnet er 13 måneder til og med måneden barnet blir 19
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.09.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNETS_ALDER                                          |                  | 05.10.2024 | 05.04.2025 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.10.2024 | 31.04.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Splitt av alders vilkåret skal ikke påvirke andelene som om det hadde vært en sammenhengende periode
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 15.02.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNETS_ALDER                                          |                  | 15.02.2024 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2021 |
      | 2       | BARNETS_ALDER                                          |                  | 01.08.2024 | 15.09.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.03.2024 | 15.09.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: For barn født 15. januar 2023 skal aldersvilkår splittes i august 2024
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 15.01.2023  |

    Når vi oppretter vilkårresultater for behandling 1

    Så forvent følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Regelsett       | Vurderes etter   | Er automatisk vurdert |
      | 1       | BOSATT_I_RIKET                                         |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 1       | MEDLEMSKAP                                             |                  | 19.06.1993 |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.02.2024 |            | OPPFYLT      | LOV_AUGUST_2024 |                  | Nei                   |
      | 2       | BARNETS_ALDER                                          |                  | 15.01.2024 | 31.07.2024 | OPPFYLT      | LOV_AUGUST_2021 |                  | Ja                    |
      | 2       | BARNETS_ALDER                                          |                  | 01.08.2024 | 15.08.2024 | OPPFYLT      | LOV_AUGUST_2024 |                  | Ja                    |

  Scenario: Ved opprettelse av ny behandling av barn født 1. okt 2022 skal aldersvilkåret være oppfylt f.o.m. 1 oktober 2023 til 31 juli 2024
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 01.10.2022  |

    Når vi oppretter vilkårresultater for behandling 1

    Så forvent følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Regelsett       | Vurderes etter   | Er automatisk vurdert |
      | 1       | BOSATT_I_RIKET                                         |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 1       | MEDLEMSKAP                                             |                  | 19.06.1993 |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.11.2023 |            | OPPFYLT      | LOV_AUGUST_2024 |                  | Nei                   |
      | 2       | BARNETS_ALDER                                          |                  | 01.10.2023 | 31.07.2024 | OPPFYLT      | LOV_AUGUST_2021 |                  | Ja                    |

  Scenario: Ved opprettelse av ny behandling av barn født 1. des 2022 skal aldersvilkåret være oppfylt f.o.m. 1 des 2023 til 31 juli 2024
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 01.12.2022  |

    Når vi oppretter vilkårresultater for behandling 1

    Så forvent følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Regelsett       | Vurderes etter   | Er automatisk vurdert |
      | 1       | BOSATT_I_RIKET                                         |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 1       | MEDLEMSKAP                                             |                  | 19.06.1993 |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.01.2024 |            | OPPFYLT      | LOV_AUGUST_2024 |                  | Nei                   |
      | 2       | BARNETS_ALDER                                          |                  | 01.12.2023 | 31.07.2024 | OPPFYLT      | LOV_AUGUST_2021 |                  | Ja                    |

  Scenario: Ved opprettelse av ny behandling av barn født 15. feb 2023 skal aldersvilkåret være oppfylt fom 15 februar 2024 til 31 juli 2024 og fom 1 august 2024 til 15 september 2024
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 15.02.2023  |

    Når vi oppretter vilkårresultater for behandling 1

    Så forvent følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Regelsett       | Vurderes etter   | Er automatisk vurdert |
      | 1       | BOSATT_I_RIKET                                         |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 1       | MEDLEMSKAP                                             |                  | 19.06.1993 |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.03.2024 |            | OPPFYLT      | LOV_AUGUST_2024 |                  | Nei                   |
      | 2       | BARNETS_ALDER                                          |                  | 15.02.2024 | 31.07.2024 | OPPFYLT      | LOV_AUGUST_2021 |                  | Ja                    |
      | 2       | BARNETS_ALDER                                          |                  | 01.08.2024 | 15.09.2024 | OPPFYLT      | LOV_AUGUST_2021 |                  | Ja                    |

  Scenario: Ved opprettelse av ny behandling av barn født 15. juli 2023 skal aldersvilkåret være oppfylt fom 15 juli 2024 til 31 juli 2024 og fom 15 august 2024 til 15 februar 2025
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 15.07.2023  |

    Når vi oppretter vilkårresultater for behandling 1

    Så forvent følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Regelsett       | Vurderes etter   | Er automatisk vurdert |
      | 1       | BOSATT_I_RIKET                                         |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 1       | MEDLEMSKAP                                             |                  | 19.06.1993 |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.08.2024 |            | OPPFYLT      | LOV_AUGUST_2024 |                  | Nei                   |
      | 2       | BARNETS_ALDER                                          |                  | 15.07.2024 | 31.07.2024 | OPPFYLT      | LOV_AUGUST_2021 |                  | Ja                    |
      | 2       | BARNETS_ALDER                                          |                  | 15.08.2024 | 15.02.2025 | OPPFYLT      | LOV_AUGUST_2021 |                  | Ja                    |

  Scenario: Ved opprettelse av ny behandling av barn født 15. august 2023 skal aldersvilkåret være oppfylt fom 15.september 2024 til tom 15. mars 2025
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 15.08.2023  |

    Når vi oppretter vilkårresultater for behandling 1

    Så forvent følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Regelsett       | Vurderes etter   | Er automatisk vurdert |
      | 1       | BOSATT_I_RIKET                                         |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 1       | MEDLEMSKAP                                             |                  | 19.06.1993 |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  |            |            | IKKE_VURDERT | LOV_AUGUST_2024 | NASJONALE_REGLER | Nei                   |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.09.2024 |            | OPPFYLT      | LOV_AUGUST_2024 |                  | Nei                   |
      | 2       | BARNETS_ALDER                                          |                  | 15.09.2024 | 15.03.2025 | OPPFYLT      | LOV_AUGUST_2021 |                  | Ja                    |
