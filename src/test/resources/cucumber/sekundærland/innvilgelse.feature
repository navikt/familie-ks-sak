# language: no
# encoding: UTF-8

Egenskap: Sekundærland - innvilgelse

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsårsak | Behandlingskategori |
      | 1            | 1        |                     | SØKNAD           | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 16.08.1988  |
      | 1            | 2       | BARN       | 15.09.2022  |

  Scenario: Skal få riktig begrunnelser for sekundærland innvilgelse
    Og følgende dagens dato 18.01.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                                                 | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD                                         |                  | 16.08.1988 |            | OPPFYLT  | Nei                  |                      |                  |
      | 1       | BOSATT_I_RIKET,MEDLEMSKAP                              |                  | 16.08.1988 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | BARNEHAGEPLASS                                         |                  | 15.09.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | MEDLEMSKAP_ANNEN_FORELDER,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 15.09.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BARNETS_ALDER                                          |                  | 15.09.2023 | 15.09.2024 | OPPFYLT  | Nei                  |                      |                  |

    Og følgende kompetanser for behandling 1
      | AktørId | Fra dato   | Til dato   | Resultat              | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.10.2023 | 30.11.2023 | NORGE_ER_SEKUNDÆRLAND | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |
      | 2       | 01.12.2023 |            | NORGE_ER_PRIMÆRLAND   | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |

    Og følgende utenlandske periodebeløp for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 01.10.2023 | 30.11.2023 | 1000  | PLN         | MÅNEDLIG  | PL              |

    Og følgende valutakurser for behandling 1
      | AktørId | Fra dato   | Til dato   | Valutakursdato | Valuta kode | Kurs         |
      | 2       | 01.10.2023 | 30.11.2023 | 2023-10-24     | PLN         | 2.6496751064 |

    Og andeler er beregnet for behandling 1

    Og når behandlingsresultatet er utledet for behandling 1

    Og vedtaksperioder er laget for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser                 | Fritekster |
      | 01.10.2023 | 30.11.2023 |                      | INNVILGET_SEKUNDÆRLAND_STANDARD |            |
      | 01.12.2023 | 31.08.2024 |                      |                                 |            |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato        | Til dato              | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | INNVILGELSE     | 1. oktober 2023 | til 30. november 2023 | 4 851 | 1                          | 15.09.22            | Du                     |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.10.2023 til 30.11.2023
      | Begrunnelse                     | Type | Barnas fødselsdatoer | Antall barn | Målform | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland | Antall timer barnehageplass |
      | INNVILGET_SEKUNDÆRLAND_STANDARD | EØS  | 15.09.22             | 1           | NB      | arbeider         | inaktiv                   | Norge                 | Polen                          | Polen               | 0                           |