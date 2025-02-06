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


  Scenario: Kontantstøtte skal innvilges i desember dersom barnet slutter i fulltids barnehageplass 31 oktober.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.08.2024  |


    Og følgende dagens dato 11.06.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2024 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 14.08.2025 | 30.10.2025 | IKKE_OPPFYLT | Nei                  |                      |                | 33           |
      | 2       | BARNEHAGEPLASS                                         |                  | 31.10.2025 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2025 | 05.03.2026 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.12.2025 | 31.02.2026 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtte skal innvilges i desember dersom barnet slutter i fulltids barnehageplass 1 november.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.08.2024  |


    Og følgende dagens dato 11.06.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2024 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 14.08.2025 | 31.10.2025 | IKKE_OPPFYLT | Nei                  |                      |                | 33           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.11.2025 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2025 | 05.03.2026 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.12.2025 | 31.02.2026 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtte skal innvilges i januar dersom barnet slutter i fulltids barnehageplass 2 november.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.08.2024  |


    Og følgende dagens dato 11.06.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2024 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 14.08.2025 | 01.11.2025 | IKKE_OPPFYLT | Nei                  |                      |                | 33           |
      | 2       | BARNEHAGEPLASS                                         |                  | 02.11.2025 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2025 | 05.03.2026 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.01.2026 | 31.02.2026 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtte skal innvilges i desember dersom barnet slutter i fulltids barnehageplass 15 oktober.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.08.2024  |


    Og følgende dagens dato 11.06.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2024 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 14.08.2025 | 14.10.2025 | IKKE_OPPFYLT | Nei                  |                      |                | 33           |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.10.2025 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2025 | 05.03.2026 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.12.2025 | 31.02.2026 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtte skal innvilges i desember dersom barnet går fra fulltid til deltid barnehageplass 31 oktober.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.08.2024  |


    Og følgende dagens dato 11.06.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2024 |            | OPPFYLT      | Nei                  |                      |                |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 14.08.2025 | 30.10.2025 | IKKE_OPPFYLT | Nei                  |                      |                | 33           |
      | 2       | BARNEHAGEPLASS                                         |                  | 31.10.2025 |            | OPPFYLT      | Nei                  |                      |                | 15           |
      | 2       | BARNETS_ALDER                                          |                  | 05.09.2025 | 05.03.2026 | OPPFYLT      | Nei                  |                      |                |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.12.2025 | 31.02.2026 | 4500  | ORDINÆR_KONTANTSTØTTE | 60     | 7500 |