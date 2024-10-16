# language: no
# encoding: UTF-8

Egenskap: Overgang til barnehageplass

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            |


  Scenario: Kontantstøtten skal opphøre fra og med november dersom barn starter i fulltids barnehage 15 oktober.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.08.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 15.10.2024 | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 16.10.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                | 40           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 31.10.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtten skal opphøre fra og med november dersom barn starter i fulltids barnehage 1 oktober.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.08.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 30.09.2024 | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.10.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                | 40           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 31.10.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtten skal reduseres fra og med oktober dersom barn starter i deltids barnehage 15 oktober.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.08.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT  | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 15.10.2024 | OPPFYLT  | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 16.10.2024 |            | OPPFYLT  | Nei                  |                      |                | 20           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT  | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 30.09.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |
      | 2       | 01.10.2024 | 31.03.2025 | 3000  | ORDINÆR_KONTANTSTØTTE | 40      | 7500 |

  Scenario: Kontantstøtten skal reduseres fra og med oktober dersom barn starter i deltids barnehage 1 oktober.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.08.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT  | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 30.09.2024 | OPPFYLT  | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.10.2024 |            | OPPFYLT  | Nei                  |                      |                | 20           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT  | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 30.09.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |
      | 2       | 01.10.2024 | 31.03.2025 | 3000  | ORDINÆR_KONTANTSTØTTE | 40      | 7500 |