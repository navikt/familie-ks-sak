# language: no
# encoding: UTF-8

Egenskap: Overgangsordning

  Scenario: Brevbegrunnelse skal bruke timer fra overgangsordning andelen hvis det finnes timer fra vilkårsvurdering i samme periode
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak      | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | LOVENDRING_2024       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | OVERGANGSORDNING_2024 | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 22.02.1992  |
      | 1            | 2       | BARN       | 01.01.2023  |
      | 2            | 1       | SØKER      | 22.02.1992  |
      | 2            | 2       | BARN       | 01.01.2023  |

    Og følgende dagens dato 17.12.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 01.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 20.05.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.01.2023 | 31.01.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 03.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 01.01.2024 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.02.2024 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 22.5         |
      | 2       | BARNETS_ALDER                |                  | 01.08.2024 | 01.08.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 01.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 20.05.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.01.2023 | 31.01.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 03.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 01.01.2024 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.02.2024 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   | 22.5         |
      | 2       | BARNETS_ALDER                |                  | 01.08.2024 | 01.08.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og følgende overgangsordning andeler for behandling 2
      | AktørId | Fra dato   | Til dato   | Delt bosted | Antall timer |
      | 2       | 01.09.2024 | 31.10.2024 | Nei         | 27           |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.02.2024 | 31.08.2024 | 3000  | ORDINÆR_KONTANTSTØTTE | 40      | 7500 | 3000                   |                          |
      | 2       | 01.09.2024 | 31.10.2024 | 1500  | OVERGANGSORDNING      | 20      | 7500 | 1500                   |                          |
    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er ENDRET_OG_OPPHØRT på behandling 2


    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.09.2024 | 31.10.2024 | UTBETALING         |           |
      | 01.11.2024 |            | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                          | Ugyldige begrunnelser |
      | 01.09.2024 | 31.10.2024 | UTBETALING         |                                | INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING |                       |
      | 01.11.2024 |            | OPPHØR             |                                | OPPHØR_OVERGANGSORDNING_OPPHØR                |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                          | Eøsbegrunnelser | Fritekster |
      | 01.09.2024 | 31.10.2024 | INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2024 til 31.10.2024
      | Begrunnelse                                   | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Målform | Gjelder andre forelder | Måned og år før vedtaksperiode |
      | INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING | STANDARD | Nei           | 01.01.23             | 1           | september 2024                       | 1 500 |                  | 27                          |         | Ja                     | august 2024                    |