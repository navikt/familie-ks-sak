# language: no
# encoding: UTF-8

Egenskap: opprettVilkårsvurdering - tester for kopiering av vilkårresultater fra forrige behandling

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 15.04.1989  |
      | 1            | 2       | BARN       | 17.01.2023  |
      | 1            | 3       | BARN       | 17.12.2022  |
      | 2            | 1       | SØKER      | 15.04.1989  |
      | 2            | 2       | BARN       | 17.01.2023  |
      | 2            | 3       | BARN       | 17.12.2022  |

  Scenario: Barnets aldervilkår skal splittes etter regelverksendring ved revurdering
    Og følgende dagens dato 27.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Fra dato   | Til dato   | Resultat | Vurderes etter   | Antall timer |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                              | 15.04.1989 |            | OPPFYLT  | NASJONALE_REGLER |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET | 17.01.2023 |            | OPPFYLT  | NASJONALE_REGLER |              |
      | 2       | BARNEHAGEPLASS                                         | 17.01.2023 |            | OPPFYLT  |                  | 40           |
      | 2       | BARNETS_ALDER                                          | 17.01.2024 | 17.01.2025 | OPPFYLT  |                  |              |

    Når vi oppretter vilkårresultater for behandling 2

    Så forvent følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                                 | Fra dato   | Til dato   | Resultat | Vurderes etter   | Antall timer | Er automatisk vurdert |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                              | 15.04.1989 |            | OPPFYLT  | NASJONALE_REGLER |              |                       |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET | 17.01.2023 |            | OPPFYLT  | NASJONALE_REGLER |              |                       |
      | 2       | BARNEHAGEPLASS                                         | 17.01.2023 |            | OPPFYLT  |                  | 40           |                       |
      | 2       | BARNETS_ALDER                                          | 17.01.2024 | 31.07.2024 | OPPFYLT  |                  |              | Ja                    |
      | 2       | BARNETS_ALDER                                          | 01.08.2024 | 17.08.2024 | OPPFYLT  |                  |              | Ja                    |

  Scenario: Ved revurdering av adopsjonsak, skal barnets aldervilkår skal splittes på regelendringsdato, men datoer skal ikke endres
    Og følgende dagens dato 27.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Vurderes etter   | Antall timer |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                              |                  | 15.04.1989 |            | OPPFYLT  | NASJONALE_REGLER |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 17.01.2023 |            | OPPFYLT  | NASJONALE_REGLER |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 17.01.2023 |            | OPPFYLT  |                  |              |
      | 2       | BARNETS_ALDER                                          | ADOPSJON         | 17.01.2024 | 17.01.2025 | OPPFYLT  |                  |              |

    Når vi oppretter vilkårresultater for behandling 2

    Så forvent følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Vurderes etter   | Antall timer |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                              |                  | 15.04.1989 |            | OPPFYLT  | NASJONALE_REGLER |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 17.01.2023 |            | OPPFYLT  | NASJONALE_REGLER |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 17.01.2023 |            | OPPFYLT  |                  |              |
      | 2       | BARNETS_ALDER                                          | ADOPSJON         | 17.01.2024 | 31.07.2024 | OPPFYLT  |                  |              |
      | 2       | BARNETS_ALDER                                          | ADOPSJON         | 01.08.2024 | 17.01.2025 | OPPFYLT  |                  |              |

  Scenario: Ved kopiering av vilkårresultater skal avslag og opphør for barnehageplassvilkåret beholdes fra forrige behanlding
    Og følgende dagens dato 27.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser             | Vurderes etter   | Antall timer |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                              | 15.04.1989 |            | OPPFYLT      | Nei                  |                                  | NASJONALE_REGLER |              |

      | 3       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET | 17.12.2022 |            | OPPFYLT      | Nei                  |                                  | NASJONALE_REGLER |              |
      | 3       | BARNEHAGEPLASS                                         | 17.12.2022 | 16.02.2023 | IKKE_OPPFYLT | Nei                  |                                  |                  | 40           |
      | 3       | BARNEHAGEPLASS                                         | 17.02.2023 | 31.04.2024 | IKKE_OPPFYLT | Ja                   | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE |                  | 40           |
      | 3       | BARNETS_ALDER                                          | 17.12.2023 | 17.12.2024 | OPPFYLT      | Nei                  |                                  |                  |              |
      | 3       | BARNEHAGEPLASS                                         | 01.05.2024 |            | OPPFYLT      | Nei                  |                                  |                  |              |

    Når vi oppretter vilkårresultater for behandling 2

    Så forvent følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                                 | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Antall timer | Er automatisk vurdert |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                              | 15.04.1989 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |              |                       |

      | 3       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET | 17.12.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |              |                       |
      | 3       | BARNEHAGEPLASS                                         | 17.12.2022 | 16.02.2023 | IKKE_OPPFYLT | Nei                  |                      |                  | 40           |                       |
      | 3       | BARNEHAGEPLASS                                         | 17.02.2023 | 31.04.2024 | IKKE_OPPFYLT | Ja                   |                      |                  | 40           |                       |
      | 3       | BARNETS_ALDER                                          | 17.12.2023 | 31.07.2024 | OPPFYLT      |                      |                      |                  |              | Ja                    |
      | 3       | BARNEHAGEPLASS                                         | 01.05.2024 |            | OPPFYLT      | Nei                  |                      |                  |              |                       |

  Scenario: Ved kopiering av vilkårresultater skal avslag og opphør fjernes for andre vilkår enn barnehageplassvilkåret
    Og følgende dagens dato 27.06.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Vurderes etter   | Søker har meldt fra om barnehageplass |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                              |                  | 15.04.1989 |            | OPPFYLT      | Nei                  | NASJONALE_REGLER | Nei                                   |

      | 3       | BARNEHAGEPLASS                                         |                  | 17.12.2022 |            | OPPFYLT      | Nei                  | NASJONALE_REGLER | Nei                                   |
      | 3       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 17.12.2022 | 16.02.2023 | IKKE_OPPFYLT | Nei                  |                  | Nei                                   |
      | 3       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 17.02.2023 | 31.04.2024 | IKKE_OPPFYLT | Ja                   |                  | Nei                                   |
      | 3       | BARNETS_ALDER                                          |                  | 17.12.2023 | 17.12.2024 | OPPFYLT      | Nei                  |                  | Nei                                   |
      | 3       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 17.12.2022 |            | OPPFYLT      | Nei                  |                  | Nei                                   |

    Når vi oppretter vilkårresultater for behandling 2

    Så forvent følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   | Søker har meldt fra om barnehageplass | Er automatisk vurdert |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                              |                  | 15.04.1989 |            | OPPFYLT  | Nei                  | NASJONALE_REGLER | Nei                                   |                       |

      | 3       | BARNEHAGEPLASS                                         |                  | 17.12.2022 |            | OPPFYLT  | Nei                  | NASJONALE_REGLER | Nei                                   |                       |
      | 3       | BARNETS_ALDER                                          |                  | 17.12.2023 | 31.07.2024 | OPPFYLT  |                      |                  |                                       | Ja                    |
      | 3       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 17.12.2022 |            | OPPFYLT  | Nei                  |                  | Nei                                   |                       |
