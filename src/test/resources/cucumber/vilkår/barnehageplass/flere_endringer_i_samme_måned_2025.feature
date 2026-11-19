# language: no
# encoding: UTF-8

Egenskap: Flere endringer i barnehageplass i samme måned

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

  Scenario: Deltids barnehageplass noen dager midt i en måned gir redusert kontantstøtte hele måneden og full kontantstøtte fra måneden etter
    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 09.12.1989  |
      | 1            | 2       | BARN       | 02.06.2025  |

    Og følgende dagens dato 19.11.2026

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 09.12.1989 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOR_MED_SØKER,MEDLEMSKAP_ANNEN_FORELDER,BOSATT_I_RIKET |                  | 02.06.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 02.06.2025 | 17.08.2026 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 18.08.2026 | 20.08.2026 | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 27           |
      | 2       | BARNEHAGEPLASS                                         |                  | 21.08.2026 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 02.06.2026 | 02.02.2027 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.07.2026 | 31.07.2026 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 2       | 01.08.2026 | 31.08.2026 | 1500  | ORDINÆR_KONTANTSTØTTE | 20      | 7500 | 1500                   |                          |
      | 2       | 01.09.2026 | 31.01.2027 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |

    Og når behandlingsresultatet er utledet for behandling 1
    Så forvent at behandlingsresultatet er INNVILGET på behandling 1

    Og vedtaksperioder er laget for behandling 1

    Så forvent at følgende begrunnelser er gyldige for behandling 1
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser             | Ugyldige begrunnelser            |
      | 01.07.2026 | 31.07.2026 | UTBETALING         |                                | INNVILGET_IKKE_BARNEHAGE         |                                  |
      | 01.08.2026 | 31.08.2026 | UTBETALING         |                                | REDUKSJON_TILDELT_BARNEHAGEPLASS |                                  |
      | 01.09.2026 | 31.01.2027 | UTBETALING         |                                | INNVILGET_IKKE_BARNEHAGE         | REDUKSJON_TILDELT_BARNEHAGEPLASS |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser             | Eøsbegrunnelser | Fritekster |
      | 01.07.2026 | 31.07.2026 | INNVILGET_IKKE_BARNEHAGE         |                 |            |
      | 01.08.2026 | 31.08.2026 | REDUKSJON_TILDELT_BARNEHAGEPLASS |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.08.2026 til 31.08.2026
      | Begrunnelse                      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Antall timer barnehageplass | Gjelder andre forelder | Måned og år før vedtaksperiode |
      | REDUKSJON_TILDELT_BARNEHAGEPLASS | STANDARD | nei           | 02.06.25             | 1           | august 2026                          | 1 500 | 27                          | true                   | juli 2026                      |

  Scenario: Revurdering der barnet slutter i barnehagen noen dager etter oppstart i samme måned gir full kontantstøtte fra måneden etter
    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 09.12.1989  |
      | 1            | 2       | BARN       | 02.06.2025  |
      | 2            | 1       | SØKER      | 09.12.1989  |
      | 2            | 2       | BARN       | 02.06.2025  |

    Og følgende dagens dato 19.11.2026

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 09.12.1989 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOR_MED_SØKER,MEDLEMSKAP_ANNEN_FORELDER,BOSATT_I_RIKET |                  | 02.06.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 02.06.2025 | 17.08.2026 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 18.08.2026 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 27           |
      | 2       | BARNETS_ALDER                                          |                  | 02.06.2026 | 02.02.2027 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 09.12.1989 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOR_MED_SØKER,MEDLEMSKAP_ANNEN_FORELDER,BOSATT_I_RIKET |                  | 02.06.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 02.06.2025 | 17.08.2026 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 18.08.2026 | 20.08.2026 | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 27           |
      | 2       | BARNEHAGEPLASS                                         |                  | 21.08.2026 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 02.06.2026 | 02.02.2027 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.07.2026 | 31.07.2026 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 2       | 01.08.2026 | 31.01.2027 | 1500  | ORDINÆR_KONTANTSTØTTE | 20      | 7500 | 1500                   |                          |

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.07.2026 | 31.07.2026 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 2       | 01.08.2026 | 31.08.2026 | 1500  | ORDINÆR_KONTANTSTØTTE | 20      | 7500 | 1500                   |                          |
      | 2       | 01.09.2026 | 31.01.2027 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |

    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er INNVILGET på behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser     | Ugyldige begrunnelser |
      | 01.09.2026 | 31.01.2027 | UTBETALING         |                                | INNVILGET_IKKE_BARNEHAGE |                       |
