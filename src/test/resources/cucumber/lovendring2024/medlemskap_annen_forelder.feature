# language: no
# encoding: UTF-8

Egenskap: Medlemskap annen forelder

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            |


  Scenario: Man skal ha rett til kontantstøtte fra og med måned man har fått innvilget vilkåret medlemskap annen forelder til og med siste måned.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.08.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP    |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 1       | LOVLIG_OPPHOLD               |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 05.11.2024 | 05.12.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNEHAGEPLASS               |                  | 05.02.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNETS_ALDER                |                  | 05.09.2024 | 05.03.2025 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.11.2024 | 31.12.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Dersom medlemskap annen forelder ikke er aktuelt skal dette regnes ut som at vilkåret er oppfylt.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.08.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP    |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 1       | LOVLIG_OPPHOLD               |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 05.11.2024 | 05.12.2024 | IKKE_AKTUELT | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNEHAGEPLASS               |                  | 05.02.2024 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNETS_ALDER                |                  | 05.09.2024 | 05.03.2025 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.11.2024 | 31.12.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |