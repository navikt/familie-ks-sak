# language: no
# encoding: UTF-8

Egenskap: Klagebehandling med endret ytelse

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | KLAGE            | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 25.10.1991  |
      | 1            | 2       | BARN       | 08.02.2023  |
      | 2            | 1       | SØKER      | 25.10.1991  |
      | 2            | 2       | BARN       | 08.02.2023  |

  Scenario: Når vi har en klage der vi øker kontantstøtte og samtidig ikke har fremtidig opphør lenger
    Og følgende dagens dato 21.05.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass |
      | 1       | BOSATT_I_RIKET               |                  | 25.10.1991 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |
      | 1       | MEDLEMSKAP                   |                  | 25.10.1996 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 02.01.2000 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 08.02.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |
      | 2       | BARNEHAGEPLASS               |                  | 08.02.2023 | 31.03.2024 | OPPFYLT  | Nei                  |                      |                  | Ja                                    |
      | 2       | BARNETS_ALDER                |                  | 08.02.2024 | 08.02.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 25.10.1991 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 25.10.1996 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 02.01.2000 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 08.02.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 08.02.2023 | 31.03.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 08.02.2024 | 08.02.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.04.2024 |            | OPPFYLT  | Nei                  |                      |                  | Ja                                    | 18           |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.03.2024 | 31.03.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 2       | 01.04.2024 | 28.02.2025 | 3000  | ORDINÆR_KONTANTSTØTTE | 40      | 7500 | 3000                   |                          |
    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er INNVILGET på behandling 2

    Og vedtaksperioder er laget for behandling 2
    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser       | Ugyldige begrunnelser |
      | 01.04.2024 | 28.02.2025 | UTBETALING         |                                | INNVILGET_DELTID_BARNEHAGE |                       |
