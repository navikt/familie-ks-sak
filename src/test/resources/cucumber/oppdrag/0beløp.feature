# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - I6yGWxXY5G

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 25.04.1990  |              |
      | 1            | 2       | BARN       | 02.12.2015  |              |
      | 1            | 3       | BARN       | 12.03.2021  |              |
      | 1            | 4       | BARN       | 03.05.2023  |              |
      | 2            | 1       | SØKER      | 25.04.1990  |              |
      | 2            | 2       | BARN       | 02.12.2015  |              |
      | 2            | 3       | BARN       | 12.03.2021  |              |
      | 2            | 4       | BARN       | 03.05.2023  |              |

  Scenario: Plassholdertekst for scenario - ry2n1BoV9s
    Og dagens dato er 28.11.2024
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 4       |
      | 2            | 3       |
      | 2            | 2       |
      | 2            | 1       |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD                          |                  | 23.04.2024 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | GIFT_PARTNERSKAP                            |                  | 02.12.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                  | 02.12.2015 | 01.12.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP                            |                  | 12.03.2021 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                                 |                  | 12.03.2021 | 11.03.2039 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | UNDER_18_ÅR                                 |                  | 03.05.2023 | 02.05.2041 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 03.05.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | GIFT_PARTNERSKAP                            |                  | 03.05.2023 |            | OPPFYLT  | Nei                  |                      |                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD                          |                  | 23.04.2024 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR                                 |                  | 02.12.2015 | 01.12.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 02.12.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 12.03.2021 | 11.03.2039 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 12.03.2021 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                               |                  | 01.03.2022 | 24.10.2024 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                               | DELT_BOSTED      | 25.10.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | UNDER_18_ÅR                                 |                  | 03.05.2023 | 02.05.2041 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | GIFT_PARTNERSKAP                            |                  | 03.05.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 03.05.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                               |                  | 03.05.2023 | 24.10.2024 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                               | DELT_BOSTED      | 25.10.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 4       | 2            | 01.11.2024 | 30.11.2024 | DELT_BOSTED | 100     | 21.11.2024       | 2024-10-25                  |
      | 1       | 2            | 01.11.2024 | 30.11.2024 | DELT_BOSTED | 100     | 21.10.2024       | 2024-10-25                  |
      | 3       | 2            | 01.11.2024 | 30.11.2024 | DELT_BOSTED | 100     | 21.11.2024       | 2024-10-25                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.05.2024 | 30.04.2041 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.09.2024 | 30.11.2033 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.04.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 1            | 01.07.2023 | 28.02.2039 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.06.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 1            | 01.07.2023 | 30.04.2041 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

      | 1       | 2            | 01.05.2024 | 31.10.2024 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.11.2024 | 30.11.2024 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.12.2024 | 30.11.2033 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.12.2033 | 30.04.2041 | 1258  | UTVIDET_BARNETRYGD | 50      | 2516 |
      | 2       | 2            | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 2            | 01.09.2024 | 30.11.2033 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.04.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 2            | 01.07.2023 | 30.11.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.12.2024 | 28.02.2039 | 883   | ORDINÆR_BARNETRYGD | 50      | 1766 |
      | 4       | 2            | 01.06.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 2            | 01.07.2023 | 30.11.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 2            | 01.12.2024 | 30.04.2041 | 883   | ORDINÆR_BARNETRYGD | 50      | 1766 |

    Når vedtaksperiodene genereres for behandling 2


    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                                                             | Ugyldige begrunnelser |
      | 01.11.2024 | 30.11.2024 | UTBETALING         |                                | INNVILGET_DATO_SKRIFTLIG_AVTALE_DELT_BOSTED, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_FULL_UTBETALING_FØR_SOKNAD_NY |                       |
      | 01.12.2024 | 30.11.2033 | UTBETALING         |                                | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED                                                                   |                       |
      | 01.12.2033 | 28.02.2039 | UTBETALING         |                                |                                                                                                                  |                       |
      | 01.03.2039 | 30.04.2041 | UTBETALING         |                                |                                                                                                                  |                       |
      | 01.05.2041 |            | OPPHØR             |                                |                                                                                                                  |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                                                             | Eøsbegrunnelser | Fritekster |
      | 01.11.2024 | 30.11.2024 | INNVILGET_DATO_SKRIFTLIG_AVTALE_DELT_BOSTED, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_FULL_UTBETALING_FØR_SOKNAD_NY |                 |            |
      | 01.12.2024 | 30.11.2033 | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED                                                                   |                 |            |
      | 01.12.2033 | 28.02.2039 |                                                                                                                  |                 |            |
      | 01.03.2039 | 30.04.2041 |                                                                                                                  |                 |            |
      | 01.05.2041 |            |                                                                                                                  |                 |            |

    Så forvent følgende brevperioder for behandling 2
      | Brevperiodetype | Fra dato      | Til dato      | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      |                 | november 2024 | november 2024 |       |                            |                     |                        |
      |                 | desember 2024 | november 2033 |       |                            |                     |                        |
      |                 | desember 2033 | februar 2039  |       |                            |                     |                        |
      |                 | mars 2039     | april 2041    |       |                            |                     |                        |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.12.2024 til 30.11.2033
      | Begrunnelse                                    | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED | STANDARD |               |                      |             |                                      |         |       |                  |                         |                             |