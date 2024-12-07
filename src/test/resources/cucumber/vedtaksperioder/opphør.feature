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
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.04.2024 | 31.07.2024 | UTBETALING         |                                |                      |                       |
      | 01.08.2024 |            | OPPHØR             |                                |                      |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |
      | 01.04.2024 | 31.07.2024 |                      |                 |            |
      | 01.08.2024 |            |                      |                 |            |