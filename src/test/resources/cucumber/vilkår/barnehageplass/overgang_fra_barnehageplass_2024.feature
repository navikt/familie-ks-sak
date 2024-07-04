# language: no
# encoding: UTF-8

Egenskap: Overgang fra barnehageplass

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            |


  Scenario: Kontantstøtte skal innvilges i oktober dersom barnet slutter i fulltids barnehageplass 15 oktober.
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 15.10.2024 | IKKE_OPPFYLT | Nei                  |                      |                | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 16.10.2024 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.10.2024 | 31.03.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtte skal innvilges i november dersom barnet slutter i fulltids barnehageplass 31 oktober.
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 31.10.2024 | IKKE_OPPFYLT | Nei                  |                      |                | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.11.2024 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.11.2024 | 31.03.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtten skal innvilges og økes fra og med oktober dersom barn går fra 40 timer til 15 timer 16 oktober.
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 15.10.2024 | IKKE_OPPFYLT | Nei                  |                      |                | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 16.10.2024 |            | OPPFYLT      | Nei                  |                      |                | 15           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.10.2024 | 31.03.2025 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |

  Scenario: Kontantstøtten skal innvilges og økes fra og med oktober dersom barn går fra 40 timer til 15 timer 31 oktober.
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 30.10.2024 | IKKE_OPPFYLT | Nei                  |                      |                | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 31.10.2024 |            | OPPFYLT      | Nei                  |                      |                | 15           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.10.2024 | 31.03.2025 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |

  Scenario: Kontantstøtten skal innvilges fra og med november dersom barnet har fulltid barnehageplass til og med 31 oktober
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 31.10.2024 | IKKE_OPPFYLT | Nei                  |                      |                | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.11.2024 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.11.2024 | 31.03.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtten skal innvilges fra og med november dersom barnet har fulltid barnehageplass til og med 31 oktober og har gradert barnehageplass fra og med 1 november
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 31.10.2024 | IKKE_OPPFYLT | Nei                  |                      |                | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.11.2024 |            | OPPFYLT      | Nei                  |                      |                | 15           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.11.2024 | 31.03.2025 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |