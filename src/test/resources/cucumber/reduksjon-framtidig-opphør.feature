# language: no
# encoding: UTF-8

Egenskap: Reduksjon framtidig opphør barnehageplass - søker melder om framtidig barnehageplass for 1 av 2 barn

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

  Scenario: Framtidig barnehageplass for 1 av 2 barn skal legge til begrunnelsen reduksjon framtidig opphør barnehageplass automatisk, og den skal ikke være mulig å velge selv eller velges bort. Fortsatt løpende kontantstøtte for det andre barnet
    Og følgende dagens dato 13.02.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                              |                  | 09.12.1989 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 05.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 05.01.2024 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 05.01.2025 | 05.09.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

      | 3       | MEDLEMSKAP_ANNEN_FORELDER,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 09.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BARNEHAGEPLASS                                         |                  | 09.01.2024 | 31.05.2025 | OPPFYLT  | Nei                  |                      |                  | Ja                                    |              |
      | 3       | BARNETS_ALDER                                          |                  | 09.01.2025 | 09.09.2025 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.02.2025 | 31.08.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 3       | 01.02.2025 | 31.05.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
    Og når behandlingsresultatet er utledet for behandling 1
    Så forvent at behandlingsresultatet er INNVILGET på behandling 1


    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.02.2025 | 31.05.2025 | UTBETALING         |           |
      | 01.06.2025 | 31.08.2025 | UTBETALING         |           |
      | 01.09.2025 |            | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 1
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser     | Ugyldige begrunnelser                     |
      | 01.02.2025 | 31.05.2025 | UTBETALING         |                                | INNVILGET_IKKE_BARNEHAGE | REDUKSJON_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS |
      | 01.06.2025 | 31.08.2025 | UTBETALING         |                                |                          | REDUKSJON_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.06.2025 til 31.08.2025
      | Begrunnelse                               | Type     | Antall barn | Barnas fødselsdatoer | Gjelder søker | Beløp | Måned og år begrunnelsen gjelder for | Gjelder andre forelder | Antall timer barnehageplass | Måned og år før vedtaksperiode |
      | REDUKSJON_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS | STANDARD | 1           | 09.01.24             | nei           | 7 500 | juni 2025                            | true                   | 0                           | mai 2025                       |