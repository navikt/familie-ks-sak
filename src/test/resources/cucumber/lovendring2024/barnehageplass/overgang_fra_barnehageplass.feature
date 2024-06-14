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


  Scenario: Kontantstøtte skal innvilges i juni dersom barnet slutter i fulltids barnehageplass 15 juni.
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 | 15.06.2024 | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 16.06.2024 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.02.2024 | 05.08.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.06.2024 | 31.08.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtte skal innvilges i juli dersom barnet slutter i fulltids barnehageplass 31 juni.
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 | 31.06.2024 | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.07.2024 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.02.2024 | 05.08.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.07.2024 | 31.08.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtten skal innvilges og økes fra og med juni dersom barn går fra 40 timer til 15 timer 16 juni.
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 | 15.06.2024 | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 16.06.2024 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 | 15           |
      | 2       | BARNETS_ALDER                                          |                  | 05.02.2024 | 05.08.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.06.2024 | 31.08.2024 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |

  Scenario: Kontantstøtten skal innvilges og økes fra og med juni dersom barn går fra 40 timer til 15 timer 30 juni.
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 | 29.06.2024 | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 30.06.2024 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 | 15           |
      | 2       | BARNETS_ALDER                                          |                  | 05.02.2024 | 05.08.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.06.2024 | 31.08.2024 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |

  Scenario: Kontantstøtten skal innvilges fra og med juli dersom barnet har fulltid barnehageplass til og med 30 juni
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 | 30.06.2024 | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.07.2024 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.02.2024 | 05.08.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.07.2024 | 31.08.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtten skal innvilges fra og med juli dersom barnet har fulltid barnehageplass til og med 30 juni
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 | 30.06.2024 | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.07.2024 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.02.2024 | 05.08.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.07.2024 | 31.08.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtten skal innvilges fra og med juli dersom barnet har fulltid barnehageplass til og med 30 juni og har gradert barnehageplass fra og med 1 juli
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.02.2024 | 30.06.2024 | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.07.2024 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 | 15           |
      | 2       | BARNETS_ALDER                                          |                  | 05.02.2024 | 05.08.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.07.2024 | 31.08.2024 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |