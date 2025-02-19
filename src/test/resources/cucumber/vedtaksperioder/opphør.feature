# language: no
# encoding: UTF-8

Egenskap: Opphørsperiode

  Scenario: Det skal ikke genereres utbetalingsperiode med 0 for perioder som har blitt nullet ut grunnet barnehage 2024
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 10.10.1994  |
      | 1            | 2       | BARN       | 20.03.2023  |

    Og følgende dagens dato 07.12.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | MEDLEMSKAP,BOSATT_I_RIKET                              |                  | 10.10.1994 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 20.03.2023 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 20.03.2023 | 14.08.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 20.03.2024 | 31.07.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 01.08.2024 | 20.10.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 15.08.2024 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 40           |

    Og følgende endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak                                 | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.08.2024 | 31.08.2024 | FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 | 0       | 15.07.2024       |                             |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.04.2024 | 31.07.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 2       | 01.08.2024 | 31.08.2024 | 0     | ORDINÆR_KONTANTSTØTTE | 0       | 7500 | 0                      |                          |
    Og når behandlingsresultatet er utledet for behandling 1
    Så forvent at behandlingsresultatet er DELVIS_INNVILGET_OG_OPPHØRT på behandling 1


    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.04.2024 | 31.07.2024 | UTBETALING         |           |
      | 01.08.2024 |            | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 1
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                          | Ugyldige begrunnelser |
      | 01.04.2024 | 31.07.2024 | UTBETALING         |                                |                                               |                       |
      | 01.08.2024 |            | OPPHØR             |                                | OPPHØR_TILLEGSTEKST_FOR_REGLER_FØR_01_08_2024 |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                          | Eøsbegrunnelser | Fritekster |
      | 01.04.2024 | 31.07.2024 |                                               |                 |            |
      | 01.08.2024 |            | OPPHØR_TILLEGSTEKST_FOR_REGLER_FØR_01_08_2024 |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.08.2024 til -
      | Begrunnelse                                   | Type     | Antall barn | Barnas fødselsdatoer | Gjelder søker | Beløp | Måned og år begrunnelsen gjelder for | Gjelder andre forelder | Antall timer barnehageplass | Måned og år før vedtaksperiode |
      | OPPHØR_TILLEGSTEKST_FOR_REGLER_FØR_01_08_2024 | STANDARD | 1           | 20.03.23             | Nei           | 0     | august 2024                          | true                   | 0                           | juli 2024                      |


  Scenario: Opphørsperioder som oppstår i mellom perioder skal ha tom dato, opphørsperioder som oppstår
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 13.12.1995  |
      | 1            | 2       | BARN       | 06.12.2022  |

    Og følgende dagens dato 07.12.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 13.12.1995 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER |                  | 06.12.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 06.12.2022 | 05.03.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 06.12.2023 | 31.07.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 06.03.2024 | 21.05.2024 | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 40           |
      | 2       | BARNEHAGEPLASS                                         |                  | 22.05.2024 |            | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.01.2024 | 29.02.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 2       | 01.06.2024 | 31.07.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
    Og når behandlingsresultatet er utledet for behandling 1

    Så forvent at behandlingsresultatet er INNVILGET_OG_OPPHØRT på behandling 1

    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.01.2024 | 29.02.2024 | UTBETALING         |           |
      | 01.03.2024 | 31.05.2024 | OPPHØR             |           |
      | 01.06.2024 | 31.07.2024 | UTBETALING         |           |
      | 01.08.2024 |            | OPPHØR             |           |


  Scenario: Opphørsperioder som starter og slutter samtidig som avslagsperioder skal bli filtrert ut.
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | SØKNAD           | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 21.03.1997  |
      | 1            | 2       | BARN       | 18.01.2023  |
      | 2            | 1       | SØKER      | 21.03.1997  |
      | 2            | 2       | BARN       | 18.01.2023  |


    Og følgende dagens dato 07.12.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 21.03.1997 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 18.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 18.01.2023 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 18.01.2024 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 01.08.2024 | 18.08.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 21.03.1997 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,MEDLEMSKAP_ANNEN_FORELDER |                  | 18.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS                                         |                  | 18.01.2023 |            | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 18.01.2024 | 31.07.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                                          |                  | 01.08.2024 | 18.08.2024 | OPPFYLT  | Nei                  |                      |                  | Nei                                   |              |

    Og følgende endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak                                 | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted | Er eksplisitt avslag |
      | 2       | 1            | 01.08.2024 | 31.08.2024 | FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 | 0       | 04.12.2024       |                             | Nei                  |
      | 2       | 2            | 01.08.2024 | 31.08.2024 | FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 | 0       | 04.12.2024       |                             | Ja                   |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.02.2024 | 31.07.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 2       | 01.08.2024 | 31.08.2024 | 0     | ORDINÆR_KONTANTSTØTTE | 0       | 7500 | 0                      |                          |
    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er AVSLÅTT på behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.08.2024 |          | AVSLAG             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                         | Ugyldige begrunnelser |
      | 01.08.2024 |          | AVSLAG             |                                | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                         | Eøsbegrunnelser | Fritekster |
      | 01.08.2024 |          |                                              |                 |            |
      | 01.08.2024 |          | AVSLAG_FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 |                 |            |


  Scenario: Ikke aktuelt vilkår som ender rett før en opphørsperiode skal være regnes som utgjørende vilkår
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | NYE_OPPLYSNINGER | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 04.08.1983  |
      | 1            | 2       | BARN       | 01.05.2022  |
      | 1            | 3       | BARN       | 29.11.2023  |
      | 2            | 1       | SØKER      | 04.08.1983  |
      | 2            | 2       | BARN       | 01.05.2022  |

    Og følgende dagens dato 13.02.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                             | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 27.01.2000 |            | OPPFYLT      | Nei                  |                                                  | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 27.01.2005 |            | OPPFYLT      | Nei                  |                                                  | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  |            |            | IKKE_AKTUELT | Nei                  |                                                  | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.05.2022 |            | OPPFYLT      | Nei                  |                                                  |                  | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 01.05.2022 |            | OPPFYLT      | Nei                  |                                                  | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 01.05.2023 | 01.05.2024 | OPPFYLT      | Nei                  |                                                  |                  | Nei                                   |              |

      | 3       | MEDLEMSKAP_ANNEN_FORELDER    |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_DEN_ANDRE_FORELDEREN_IKKE_MEDLEM_I_FEM_ÅR | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 29.11.2023 |            | OPPFYLT      | Nei                  |                                                  | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               |                  | 29.11.2023 |            | OPPFYLT      | Nei                  |                                                  |                  | Nei                                   |              |
      | 3       | BARNETS_ALDER                |                  | 29.12.2024 | 29.06.2025 | OPPFYLT      | Nei                  |                                                  |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 27.01.2000 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 27.01.2005 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | BARNEHAGEPLASS               |                  | 01.05.2022 |            | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 01.05.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 01.05.2022 | 16.11.2023 | IKKE_AKTUELT | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 01.05.2023 | 01.05.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 17.11.2023 |            | IKKE_OPPFYLT | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.06.2023 | 31.10.2023 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er OPPHØRT på behandling 2


    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.11.2023 |          | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                    | Ugyldige begrunnelser |
      | 01.11.2023 |          | OPPHØR             |                                | OPPHØR_ANNEN_FORELDER_IKKE_MEDLEM_FOLKETRYGDEN_I_FEM_ÅR |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                                    | Eøsbegrunnelser | Fritekster |
      | 01.11.2023 |          | OPPHØR_ANNEN_FORELDER_IKKE_MEDLEM_FOLKETRYGDEN_I_FEM_ÅR |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.11.2023 til -
      | Begrunnelse                                             | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Antall timer barnehageplass | Målform | Gjelder andre forelder | Måned og år før vedtaksperiode |
      | OPPHØR_ANNEN_FORELDER_IKKE_MEDLEM_FOLKETRYGDEN_I_FEM_ÅR | STANDARD |               | 01.05.22             | 1           | november 2023                        | 0     |                  | 0                           |         |                        | oktober 2023                   |

  Scenario: Det skal opprettes riktig opphørsperiode når man går fra noe andeler til ingen andeler
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | NYE_OPPLYSNINGER | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 12.10.1992  |
      | 1            | 2       | BARN       | 02.06.2022  |
      | 1            | 3       | BARN       | 11.02.2024  |
      | 2            | 1       | SØKER      | 12.10.1992  |
      | 2            | 2       | BARN       | 02.06.2022  |
      | 2            | 3       | BARN       | 11.02.2024  |
    Og følgende dagens dato 19.02.2025

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 18.09.2001 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 18.09.2006 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 04.07.2000 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 02.06.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 02.06.2022 | 31.07.2023 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 02.06.2023 | 02.06.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.08.2023 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 50           |

      | 3       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 04.07.2000 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 11.02.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               |                  | 11.02.2024 | 31.03.2025 | OPPFYLT      | Nei                  |                      |                  | Ja                                    |              |
      | 3       | BARNETS_ALDER                |                  | 11.02.2025 | 11.10.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | BOSATT_I_RIKET               |                  | 18.09.2001 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 1       | MEDLEMSKAP                   |                  | 18.09.2006 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 04.07.2000 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 02.06.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 02.06.2022 | 31.07.2023 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER                |                  | 02.06.2023 | 02.06.2024 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNEHAGEPLASS               |                  | 01.08.2023 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 50           |

      | 3       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 04.07.2000 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               |                  | 11.02.2024 | 28.02.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 3       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 11.02.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER | Nei                                   |              |
      | 3       | BARNETS_ALDER                |                  | 11.02.2025 | 11.10.2025 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 3       | BARNEHAGEPLASS               |                  | 01.03.2025 |            | IKKE_OPPFYLT | Nei                  |                      |                  | Nei                                   | 33           |

    Og andeler er beregnet for behandling 1
    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.07.2023 | 31.07.2023 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
      | 3       | 01.03.2025 | 31.03.2025 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.07.2023 | 31.07.2023 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 | 7500                   |                          |
    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er OPPHØRT på behandling 2

    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.08.2023 |            | OPPHØR             |           |
      | 01.03.2025 | 31.03.2025 | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser             | Ugyldige begrunnelser |
      | 01.08.2023 |            | OPPHØR             |                                | OPPHØR_FULLTIDSPLASS_I_BARNEHAGE |                       |
      | 01.03.2025 | 31.03.2025 | OPPHØR             |                                | OPPHØR_FULLTIDSPLASS_I_BARNEHAGE |                       |


    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser             | Eøsbegrunnelser | Fritekster |
      | 01.03.2025 | 31.03.2025 | OPPHØR_FULLTIDSPLASS_I_BARNEHAGE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.03.2025 til 31.03.2025
      | Begrunnelse                      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Målform | Beløp | Måned og år begrunnelsen gjelder for | Gjelder andre forelder | Antall timer barnehageplass | Måned og år før vedtaksperiode |
      | OPPHØR_FULLTIDSPLASS_I_BARNEHAGE | STANDARD | Nei           | 11.02.24             | 1           |         | 0     | mars 2025                            | true                   | 33                          | februar 2025                   |