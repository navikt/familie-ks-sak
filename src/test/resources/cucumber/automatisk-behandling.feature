
# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - wvM6abwTbv

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Behandlingskategori |
      | 1            | 1        |                     | IKKE_VURDERT        | SØKNAD           | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 11.04.1987  |
      | 1            | 2       | BARN       | 02.02.2018  |
      | 1            | 3       | BARN       | 17.02.2020  |
      | 1            | 4       | BARN       | 07.05.2022  |

  Scenario: Plassholdertekst for scenario - pZFASfWRW5
    Og følgende dagens dato 10.01.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 11.04.1987 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 02.02.2018 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNEHAGEPLASS                                         |                  | 02.02.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BARNETS_ALDER                                          |                  | 02.02.2019 | 02.02.2020 | OPPFYLT  | Nei                  |                      |                  |

      | 3       | BOSATT_I_RIKET,BOR_MED_SØKER,MEDLEMSKAP_ANNEN_FORELDER |                  | 17.02.2020 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BARNEHAGEPLASS                                         |                  | 17.02.2020 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BARNETS_ALDER                                          |                  | 17.02.2021 | 17.02.2022 | OPPFYLT  | Nei                  |                      |                  |

      | 4       | BOR_MED_SØKER,BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 07.05.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BARNEHAGEPLASS                                         |                  | 07.05.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BARNETS_ALDER                                          |                  | 07.05.2023 | 07.05.2024 | OPPFYLT  | Nei                  |                      |                  |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.03.2019 | 31.01.2020 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |
      | 3       | 01.03.2021 | 31.01.2022 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |
      | 4       | 01.06.2023 | 30.04.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato | Til dato | Vedtaksperiodetype | Kommentar |

    Når vedtaksperiodene genereres for behandling 1