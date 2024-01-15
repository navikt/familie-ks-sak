# language: no
# encoding: UTF-8

Egenskap: Fortsatt innvilget

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SØKNAD           | NASJONAL            |
      | 2            | 2        | 1                   | FORTSATT_INNVILGET  | NYE_OPPLYSNINGER | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 11.04.1987  |
      | 1            | 2       | BARN       | 07.05.2022  |
      | 2            | 1       | SØKER      | 11.04.1987  |
      | 2            | 2       | BARN       | 07.05.2022  |

  Scenario: Skal få riktig begrunnelse for fortsatt innvilget
    Og følgende dagens dato 15.01.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                   | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                |                  | 11.04.1987 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 07.05.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                            |                  | 07.05.2022 | 31.12.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNEHAGEPLASS                           |                  | 07.05.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BARNETS_ALDER                            |                  | 07.05.2023 | 07.05.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                            | DELT_BOSTED      | 01.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                   | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                |                  | 11.04.1987 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | BOR_MED_SØKER                            |                  | 07.05.2022 | 31.12.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOSATT_I_RIKET |                  | 07.05.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNEHAGEPLASS                           |                  | 07.05.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BARNETS_ALDER                            |                  | 07.05.2023 | 07.05.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                            | DELT_BOSTED      | 01.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp |
      | 2       | 01.06.2023 | 31.01.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |
      | 2       | 01.02.2024 | 30.04.2024 | 3750  | ORDINÆR_KONTANTSTØTTE | 50      | 7500 | 3750                   |

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato | Til dato | Vedtaksperiodetype |
      |          |          | FORTSATT_INNVILGET |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato | Til dato | VedtaksperiodeType | Gyldige begrunnelser                  |
      |          |          | FORTSATT_INNVILGET | FORTSATT_INNVILGET_BARN_BOR_MED_SOKER |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato | Til dato | Standardbegrunnelser                  |
      |          |          | FORTSATT_INNVILGET_BARN_BOR_MED_SOKER |

    Så forvent følgende brevperioder for behandling 2
      | Brevperiodetype    | Fra dato | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager |
      | FORTSATT_INNVILGET | Du får:  |          | 7 500 | 1                          | 07.05.22            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode - til -
      | Begrunnelse                           | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Målform | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Gjelder andre forelder |
      | FORTSATT_INNVILGET_BARN_BOR_MED_SOKER | STANDARD | Nei           | 07.05.22             | 1           | NB      | 7 500 |                  | 0                           | Ja                     |