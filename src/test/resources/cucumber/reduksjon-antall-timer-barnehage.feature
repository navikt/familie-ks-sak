# language: no
# encoding: UTF-8

Egenskap: Reduksjon antall timer i barnehage

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 09.12.1989  |
      | 1            | 2       | BARN       | 05.01.2024  |
      | 1            | 3       | BARN       | 09.01.2024  |

  Scenario: Når det er reduksjon på barnet pga deltid barnehageplass, så flettes riktig barn inn i begrunnelsen
    Og følgende dagens dato 25.02.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 09.12.1989 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOR_MED_SØKER,MEDLEMSKAP_ANNEN_FORELDER,BOSATT_I_RIKET |                  | 05.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.01.2024 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.01.2025 | 05.09.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

      | 3       | BOSATT_I_RIKET,BOR_MED_SØKER,MEDLEMSKAP_ANNEN_FORELDER |                  | 09.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BARNEHAGEPLASS                                         |                  | 09.01.2024 | 28.02.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 3       | BARNEHAGEPLASS                                         |                  | 01.03.2025 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 8            |
      | 3       | BARNETS_ALDER                                          |                  | 09.01.2025 | 09.09.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.02.2025 | 31.08.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 3       | 01.02.2025 | 28.02.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 3       | 01.03.2025 | 31.08.2025 | 6000  | ORDINÆR_KONTANTSTØTTE | 80      | 7500 | 6000                   |                          |
    Og når behandlingsresultatet er utledet for behandling 1
    Så forvent at behandlingsresultatet er INNVILGET på behandling 1


    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.02.2025 | 28.02.2025 | UTBETALING         |           |
      | 01.03.2025 | 31.08.2025 | UTBETALING         |           |
      | 01.09.2025 |            | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 1
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser             | Ugyldige begrunnelser |
      | 01.02.2025 | 28.02.2025 | UTBETALING         |                                | INNVILGET_IKKE_BARNEHAGE         |                       |
      | 01.03.2025 | 31.08.2025 | UTBETALING         |                                | REDUKSJON_TILDELT_BARNEHAGEPLASS |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser             | Eøsbegrunnelser | Fritekster |
      | 01.02.2025 | 28.02.2025 | INNVILGET_IKKE_BARNEHAGE         |                 |            |
      | 01.03.2025 | 31.08.2025 | REDUKSJON_TILDELT_BARNEHAGEPLASS |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.03.2025 til 31.08.2025
      | Begrunnelse                      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp  | Antall timer barnehageplass | Gjelder andre forelder | Måned og år før vedtaksperiode |
      | REDUKSJON_TILDELT_BARNEHAGEPLASS | STANDARD | nei           | 09.01.24             | 1           | mars 2025                            | 13 500 | 8                           | true                   | februar 2025                   |