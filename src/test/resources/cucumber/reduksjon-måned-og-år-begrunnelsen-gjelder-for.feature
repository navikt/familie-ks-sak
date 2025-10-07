# language: no
# encoding: UTF-8

Egenskap: Reduksjon pga færre antall timer i barnehage etter lovendring 2025

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | NYE_OPPLYSNINGER | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 30.06.1993  |
      | 1            | 2       | BARN       | 01.01.2024  |
      | 2            | 1       | SØKER      | 30.06.1993  |
      | 2            | 2       | BARN       | 01.01.2024  |

  Scenario: Når det er reduksjon etter lovendring 2025 ønsker vi å flette inn måned og år begrunnelsen gjelder for riktig
    Og følgende dagens dato 24.02.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 30.06.1993 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 30.06.1998 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 09.05.1997 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.01.2024 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 01.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 01.01.2025 | 01.09.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 30.06.1993 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 30.06.1998 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 09.05.1997 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 01.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 01.01.2025 | 01.09.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.01.2024 | 09.03.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 10.03.2025 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 30           |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.02.2025 | 28.02.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 2       | 01.03.2025 | 31.08.2025 | 1500  | ORDINÆR_KONTANTSTØTTE | 20      | 7500 | 1500                   |                          |
    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er ENDRET_UTBETALING på behandling 2


    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.03.2025 | 31.08.2025 | UTBETALING         |           |
      | 01.09.2025 |            | OPPHØR             |           |


    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser             | Ugyldige begrunnelser |
      | 01.03.2025 | 31.08.2025 | UTBETALING         |                                | REDUKSJON_TILDELT_BARNEHAGEPLASS |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser             | Eøsbegrunnelser | Fritekster |
      | 01.03.2025 | 31.08.2025 | REDUKSJON_TILDELT_BARNEHAGEPLASS |                 |            |


    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.03.2025 til 31.08.2025
      | Begrunnelse                      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Gjelder andre forelder | Måned og år før vedtaksperiode |
      | REDUKSJON_TILDELT_BARNEHAGEPLASS | STANDARD | false         | 01.01.24             | 1           | mars 2025                            | 1 500 |                  | 30                          | true                   | februar 2025                   |