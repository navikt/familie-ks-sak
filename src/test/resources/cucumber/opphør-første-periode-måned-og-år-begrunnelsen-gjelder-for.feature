# language: no
# encoding: UTF-8

Egenskap: Opphør første periode måned og år begrunnelsen gjelder for

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
      | 1            | 1       | SØKER      | 25.02.1996  |
      | 1            | 2       | BARN       | 17.01.2024  |
      | 2            | 1       | SØKER      | 25.02.1996  |
      | 2            | 2       | BARN       | 17.01.2024  |

  Scenario: Når det er opphør første periode og måned og år begrunnelsen gjelder for er lenge før vedtaksperioden er flettefeltet fortsatt riktig
    Og følgende dagens dato 24.02.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 07.10.2014 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 07.10.2019 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  |            |            | IKKE_AKTUELT | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 17.01.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 17.01.2024 |            | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 17.02.2025 | 17.08.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 07.10.2014 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 07.10.2019 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  |            |            | IKKE_AKTUELT | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 17.01.2024 | 30.11.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 17.01.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.12.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 45           |
      | 2       | BARNETS_ALDER                |                  | 17.01.2025 | 17.09.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er OPPHØRT på behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.03.2025 |          | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                             | Ugyldige begrunnelser |
      | 01.03.2025 |          | OPPHØR             |                                | OPPHØR_FULLTIDSPLASS_I_BARNEHAGEN_FØRSTE_PERIODE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                             | Eøsbegrunnelser | Fritekster |
      | 01.03.2025 |          | OPPHØR_FULLTIDSPLASS_I_BARNEHAGEN_FØRSTE_PERIODE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.03.2025 til -
      | Begrunnelse                                      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Måned og år før vedtaksperiode |
      | OPPHØR_FULLTIDSPLASS_I_BARNEHAGEN_FØRSTE_PERIODE | STANDARD | false         | 17.01.24             | 1           | desember 2024                        | 0     |                  | 45                          | februar 2025                   |
