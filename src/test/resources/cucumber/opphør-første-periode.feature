# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - Vmua0FQfPq

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | SØKNAD           | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 10.10.1988  |
      | 1            | 2       | BARN       | 30.10.2021  |

  Scenario: Plassholdertekst for scenario - UOPAuGC7TN
    Og følgende dagens dato 10.12.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                    | Utdypende vilkår             | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   | Søker har meldt fra om barnehageplass | Antall timer |
      | 1       | MEDLEMSKAP                |                              | 10.10.1993 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 1       | BOSATT_I_RIKET            | OMFATTET_AV_NORSK_LOVGIVNING | 30.10.2021 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 1       | LOVLIG_OPPHOLD            |                              | 30.10.2021 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER |                              |            |            | IKKE_AKTUELT | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 2       | BOR_MED_SØKER             | BARN_BOR_I_EØS_MED_SØKER     | 30.10.2021 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 2       | BOSATT_I_RIKET            | BARN_BOR_I_EØS               | 30.10.2021 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN | Nei                                   |              |
      | 2       | BARNEHAGEPLASS            |                              | 30.10.2021 |            | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |
      | 2       | BARNETS_ALDER             |                              | 30.10.2022 | 30.10.2023 | OPPFYLT      | Nei                  |                      |                  | Nei                                   |              |

    Og følgende endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.11.2022 | 30.09.2023 | ETTERBETALING_3MND | 0       | 02.10.2024       |                             |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |
      | 2       | 01.11.2022 | 30.09.2023 | 0     | ORDINÆR_KONTANTSTØTTE | 0       | 7500 | 0                      |                          |
    Og når behandlingsresultatet er utledet for behandling 1
    Så forvent at behandlingsresultatet er AVSLÅTT på behandling 1


    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato | Til dato | Vedtaksperiodetype | Kommentar |

    Så forvent at følgende begrunnelser er gyldige for behandling 1
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Ugyldige begrunnelser |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |