# language: no
# encoding: UTF-8

Egenskap: Fortsatt opphørt

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | NYE_OPPLYSNINGER | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | NYE_OPPLYSNINGER | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 27.04.1977  |
      | 1            | 2       | BARN       | 07.01.2024  |
      | 2            | 1       | SØKER      | 27.04.1977  |
      | 2            | 2       | BARN       | 07.01.2024  |

  Scenario: Hvis man fikk opphør i behandling 1, og det er ikke noe forskjell i utbetaling i behandling 2, så skal man få fortsatt opphørt.
    Og følgende dagens dato 23.04.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 23.07.1981 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 23.07.1986 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 10.09.1987 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 07.01.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 07.01.2024 | 02.02.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 07.01.2025 | 07.09.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 03.02.2025 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 45           |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 23.07.1981 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 23.07.1986 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 10.09.1987 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 07.01.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 07.01.2024 | 26.01.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 07.01.2025 | 07.09.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 27.01.2025 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 45           |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |

    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er FORTSATT_OPPHØRT på behandling 2