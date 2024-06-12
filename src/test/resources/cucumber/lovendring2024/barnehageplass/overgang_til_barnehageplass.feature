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


  Scenario: Kontantstøtten skal opphøre fra og med juli dersom barn starter i fulltids barnehage 15 juni.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.01.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 | 15.06.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 16.06.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNETS_ALDER                                          |                  | 05.02.2024 | 05.08.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.02.2024 | 31.06.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtten skal opphøre fra og med august dersom barn starter i fulltids barnehage 1 juli.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.01.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 | 31.06.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.07.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNETS_ALDER                                          |                  | 05.02.2024 | 05.08.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.02.2024 | 30.07.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtten skal reduseres fra og med juli dersom barn starter i deltids barnehage 15 juni.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.01.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 | 15.06.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 16.06.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 | 20           |
      | 2       | BARNETS_ALDER                                          |                  | 05.02.2024 | 05.08.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.02.2024 | 31.06.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |
      | 2       | 01.07.2024 | 31.08.2024 | 3000  | ORDINÆR_KONTANTSTØTTE | 40      | 7500 |

  Scenario: Kontantstøtten skal reduseres fra og med august dersom barn starter i deltids barnehage 1 juli.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.01.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 | 31.06.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.07.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 | 20           |
      | 2       | BARNETS_ALDER                                          |                  | 05.02.2024 | 05.08.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.02.2024 | 31.07.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |
      | 2       | 01.08.2024 | 31.08.2024 | 3000  | ORDINÆR_KONTANTSTØTTE | 40      | 7500 |