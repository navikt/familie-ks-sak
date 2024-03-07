# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori |
      | 1            | 2        |                     | SØKNAD           | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.06.1988  |
      | 1            | 2       | BARN       | 04.02.2022  |

  Scenario: Skal få opp vedtaksperioder
    Og følgende dagens dato 11.01.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 19.06.1988 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 04.02.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BARNEHAGEPLASS                                         |                  | 04.02.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BARNETS_ALDER                                          |                  | 04.02.2023 | 04.02.2024 | OPPFYLT  | Nei                  |                      |                  |

    Og andeler er beregnet for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Ytelse type           | Prosent | Sats |
      | 2       | 01.03.2023 | 31.01.2024 | 7500  | ORDINÆR_KONTANTSTØTTE | 100     | 7500 |

    Og følgende kompetanser for behandling 1
      | AktørId | Fra dato   | Til dato | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.03.2023 |          | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | NO                             | NO                  |

    Og når behandlingsresultatet er utledet for behandling 1

    Og vedtaksperioder er laget for behandling 1

    Så forvent følgende vedtaksperioder på behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.03.2023 | 31.01.2024 | UTBETALING         |           |
      | 01.02.2024 |            | OPPHØR             |           |