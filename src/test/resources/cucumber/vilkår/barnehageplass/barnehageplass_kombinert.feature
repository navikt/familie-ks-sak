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
      | 2            | 1        | 1                   | SØKNAD           | NASJONAL            |

  Scenario: Skal få kontantstøtte i perioden barn er mellom et til to år, fra og med måneden barnet er 13 måneder til og med 24 måneder.
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 01.01.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 01.01.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.02.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |
      | 2       | BARNETS_ALDER                                          |                  | 01.02.2024 | 01.02.2025 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.03.2024 | 28.02.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontantstøtte skal innvilges i juli dersom barnet slutter i juni i gammelt regelsett, og kontantstøtten opphører i oktober dersom barnet begynner i barnehage i midten av september i nytt regelverk
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 01.03.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.05.2024 | 14.06.2024 | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.06.2024 | 14.09.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.09.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNETS_ALDER                                          |                  | 01.04.2024 | 01.10.2025 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.07.2024 | 30.09.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

  Scenario: Kontatstøtte skal innvilges i juni dersom barnet ikke har barnehageplass i mai i gammelt regelsett, reduseres fra samme måned i gammelt regelsett, reduseres måneden etter i nytt regelsett
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 01.03.2023  |

    Og følgende dagens dato 11.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.05.2024 | 14.07.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.07.2024 | 15.09.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 | 8            |
      | 2       | BARNEHAGEPLASS                                         |                  | 16.09.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 | 16           |
      | 2       | BARNETS_ALDER                                          |                  | 01.04.2024 | 01.10.2025 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.06.2024 | 30.06.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |
      | 2       | 01.07.2024 | 30.09.2024 | 6000  | ORDINÆR_KONTANTSTØTTE | 80      | 7500 |
      | 2       | 01.10.2024 | 01.10.2025 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |

  Scenario: case 1
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 20.03.2023  |
      | 2            | 1       | SØKER      | 19.06.1988  |
      | 2            | 2       | BARN       | 20.03.2023  |

    Og følgende dagens dato 01.08.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 20.03.2023 |            | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 20.04.2024 | 14.05.2024 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.05.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                | LOV_AUGUST_2024 | 40           |
      | 2       | BARNETS_ALDER                                          |                  | 20.04.2024 | 20.10.2025 | OPPFYLT      | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 20.03.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 20.04.2024 | 14.05.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.05.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 | 20           |
      | 2       | BARNETS_ALDER                                          |                  | 20.04.2024 | 20.10.2025 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1
    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |
      # Ingen andeler, er det riktig?

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.05.2024 | 31.10.2025 | 3000  | ORDINÆR_KONTANTSTØTTE | 40      | 7500 |

  Scenario: case 2
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 25.04.2023  |
      | 2            | 1       | SØKER      | 19.06.1988  |
      | 2            | 2       | BARN       | 25.04.2023  |

    Og følgende dagens dato 01.08.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 25.04.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 25.05.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNETS_ALDER                                          |                  | 25.05.2024 | 20.11.2025 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 25.04.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 25.05.2024 | 09.08.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 10.08.2024 | 30.09.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 | 30           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.10.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 | 15           |
      | 2       | BARNETS_ALDER                                          |                  | 25.05.2024 | 25.11.2025 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1
    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.06.2024 | 30.11.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.06.2024 | 31.08.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |
      | 2       | 01.09.2024 | 30.09.2024 | 1500  | ORDINÆR_KONTANTSTØTTE | 20      | 7500 |
      | 2       | 01.10.2024 | 30.11.2025 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |

  Scenario: case 3
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 25.04.2023  |
      | 2            | 1       | SØKER      | 19.06.1988  |
      | 2            | 2       | BARN       | 05.04.2023  |

    Og følgende dagens dato 01.08.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.04.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.05.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.05.2024 | 05.11.2025 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                  | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | BOSATT_I_RIKET                          |                  | 10.05.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                          |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER |                  | 05.04.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BOSATT_I_RIKET                          |                  | 10.05.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                          |                  | 10.05.2024 | 09.07.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                          |                  | 10.07.2024 | 31.08.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 | 15           |
      | 2       | BARNEHAGEPLASS                          |                  | 01.09.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 | 30           |
      | 2       | BARNETS_ALDER                           |                  | 05.05.2024 | 05.11.2025 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1
    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.06.2024 | 30.11.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.06.2024 | 31.06.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |
      | 2       | 01.07.2024 | 31.09.2024 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |
      | 2       | 01.10.2024 | 30.11.2025 | 1500  | ORDINÆR_KONTANTSTØTTE | 20      | 7500 |

  Scenario: case 4
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 05.05.2023  |

    Og følgende dagens dato 01.08.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter | Regelsett       | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.05.2023 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.06.2024 | 09.07.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 10.07.2024 | 31.08.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 | 15           |
      | 2       | BARNEHAGEPLASS                                         |                  | 01.09.2024 | 14.12.2024 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 | 30           |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.12.2024 |            | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.06.2024 | 05.12.2025 | OPPFYLT  | Nei                  |                      |                | LOV_AUGUST_2024 |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      # Mangler en andel her?
      | 2       | 01.07.2024 | 31.09.2024 | 4500  | ORDINÆR_KONTANTSTØTTE | 60      | 7500 |
      | 2       | 01.10.2024 | 30.11.2024 | 1500  | ORDINÆR_KONTANTSTØTTE | 20      | 7500 |
      | 2       | 01.12.2024 | 31.12.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |
