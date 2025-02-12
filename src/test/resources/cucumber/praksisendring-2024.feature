# language: no
# encoding: UTF-8

Egenskap:Praksisendring 2024

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | LOVENDRING_2024  | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 20.08.1992  |
      | 1            | 2       | BARN       | 13.07.2023  |
      | 2            | 1       | SØKER      | 20.08.1992  |
      | 2            | 2       | BARN       | 13.07.2023  |

  Scenario: Dersom andel type forandrer på seg fra ORDINÆR_KONTANTSTØTTE til PRAKSISENDRING_2024 så skal det ikke bli sett på som en endring i behandlingsresultatet
    Og følgende dagens dato 10.02.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 08.04.2002 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 08.04.2007 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 08.01.2021 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 13.07.2023 | 31.07.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 13.07.2023 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 13.07.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.08.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 37.5         |
      | 2       | BARNETS_ALDER                |                  | 13.08.2024 | 13.02.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                             | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 08.04.2002 |            | OPPFYLT      | Nei                  |                                                  | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 08.04.2007 |            | OPPFYLT      | Nei                  |                                                  | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 08.01.2021 |            | OPPFYLT      | Nei                  |                                                  | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 13.07.2023 |            | OPPFYLT      | Nei                  |                                                  | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 13.07.2023 | 31.07.2024 | OPPFYLT      | Nei                  |                                                  |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 13.07.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                                                  |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.08.2024 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_KOMMUNEN_MELDER_FULLTIDSPLASS_I_BARNEHAGE |                  | Nei                                   | 45           |
      | 2       | BARNETS_ALDER                |                  | 13.08.2024 | 13.02.2025 | OPPFYLT      | Nei                  |                                                  |                  | Nei                                   |              |

    Og med følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.08.2024 | 31.08.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |

    Og med følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type         | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.08.2024 | 31.08.2024 | 7500  | PRAKSISENDRING_2024 | 100     | 7500 | 7500                   |                          |

    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er AVSLÅTT på behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.09.2024 |          | AVSLAG             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                             | Ugyldige begrunnelser |
      | 01.09.2024 |          | AVSLAG             |                                | AVSLAG_KOMMUNEN_MELDER_FULLTIDSPLASS_I_BARNEHAGE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                             | Eøsbegrunnelser | Fritekster |
      | 01.09.2024 |          | AVSLAG_KOMMUNEN_MELDER_FULLTIDSPLASS_I_BARNEHAGE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2024 til -
      | Begrunnelse                                      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Gjelder andre forelder | Måned og år før vedtaksperiode |
      | AVSLAG_KOMMUNEN_MELDER_FULLTIDSPLASS_I_BARNEHAGE | STANDARD |               | 13.07.23             | 1           | august 2024                          | 0     |                  | 45                          |                        | august 2024                    |