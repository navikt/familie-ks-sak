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


  Scenario: Kontantstøtten skal opphøres fra måneden etter man går fra gradert til fulltidsbarnehageplass hvis man får barnehageplass i midten måneden
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 14.11.2024 | OPPFYLT      | Nei                  |                      |                | 15           |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.11.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                | 40           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 30.11.2024 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |

  Scenario: Kontantstøtten skal opphøres fra måneden etter man går fra gradert til fulltidsbarnehageplass hvis man får barnehageplass på den første i måneden
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 30.11.2024 | OPPFYLT      | Nei                  |                      |                | 15           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.12.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                | 40           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 31.12.2024 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |

  Scenario: Kontantstøtte skal økes samme måned som man reduserer oppholdstid når man reduserer midt i måneden
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 14.10.2024 | OPPFYLT  | Nei                  |                      |                | 15           |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.10.2024 |            | OPPFYLT  | Nei                  |                      |                | 8            |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT  | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 31.09.2024 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |
      | 2       | 01.10.2024 | 31.03.2025 | 6000  | ORDINÆR_KONTANTSTØTTE | 80      | 7500 |

  Scenario: Kontantstøtte skal økes samme måned som man reduserer oppholdstid når man reduserer den første i måneden
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 30.11.2024 | OPPFYLT  | Nei                  |                      |                | 15           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.12.2024 |            | OPPFYLT  | Nei                  |                      |                | 8            |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT  | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 31.11.2024 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |
      | 2       | 01.12.2024 | 31.03.2025 | 6000  | ORDINÆR_KONTANTSTØTTE | 80      | 7500 |

  Scenario: Kontantstøtte skal reduseres fra og med neste måned når oppholdstiden øker i midten av måneden
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 14.11.2024 | OPPFYLT  | Nei                  |                      |                | 8            |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.11.2024 |            | OPPFYLT  | Nei                  |                      |                | 15           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT  | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 30.11.2024 | 6000  | ORDINÆR_KONTANTSTØTTE | 80      | 7500 |
      | 2       | 01.12.2024 | 31.03.2025 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |

  Scenario: Kontantstøtte skal reduseres fra og med neste måned når oppholdstiden øker den første i måneden
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 31.10.2024 | OPPFYLT  | Nei                  |                      |                | 8            |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.11.2024 |            | OPPFYLT  | Nei                  |                      |                | 15           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT  | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 31.11.2024 | 6000  | ORDINÆR_KONTANTSTØTTE | 80      | 7500 |
      | 2       | 01.12.2024 | 31.03.2025 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |

  Scenario: Kontantstøtte skal økes til full fra samme måned som når barnet slutter i barnehage i mitden av måneden
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 14.11.2024 | OPPFYLT  | Nei                  |                      |                | 8            |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.11.2024 |            | OPPFYLT  | Nei                  |                      |                |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT  | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 30.10.2024 | 6000  | ORDINÆR_KONTANTSTØTTE | 80      | 7500 |
      | 2       | 01.11.2024 | 31.03.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario:  Kontantstøtte skal økes til full i samme måned hvis barnet ikke går i barnehage siste dag i måned
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 30.10.2024 | OPPFYLT  | Nei                  |                      |                | 8            |
      | 2       | BARNEHAGEPLASS                                         |                  | 31.10.2024 |            | OPPFYLT  | Nei                  |                      |                |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT  | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 30.09.2024 | 6000  | ORDINÆR_KONTANTSTØTTE | 80      | 7500 |
      | 2       | 01.10.2024 | 31.03.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario:  Kontantstøtte skal økes til full i neste måned hvis barnet går i barnehage den siste dagen i måneden
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 31.10.2024 | OPPFYLT  | Nei                  |                      |                | 8            |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.11.2024 |            | OPPFYLT  | Nei                  |                      |                |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT  | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 31.10.2024 | 6000  | ORDINÆR_KONTANTSTØTTE | 80      | 7500 |
      | 2       | 01.11.2024 | 31.03.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtte skal økes til full i samme måned når barnet slutter i barnehage den første i måneden
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
      | 2       | BARNEHAGEPLASS                                         |                  | 05.09.2024 | 01.11.2024 | OPPFYLT  | Nei                  |                      |                | 8            |
      | 2       | BARNEHAGEPLASS                                         |                  | 02.11.2024 |            | OPPFYLT  | Nei                  |                      |                |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2024 | 05.03.2025 | OPPFYLT  | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.09.2024 | 30.10.2024 | 6000  | ORDINÆR_KONTANTSTØTTE | 80      | 7500 |
      | 2       | 01.11.2024 | 31.03.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |