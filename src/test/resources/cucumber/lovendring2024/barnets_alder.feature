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
      | 1            | 2       | BARN       | 05.05.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNETS_ALDER                                          |                  | 05.06.2024 | 05.12.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.06.2024 | 31.12.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: For barn født 15. januar 2023 skal aldersvilkår splittes i agust 2024
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
