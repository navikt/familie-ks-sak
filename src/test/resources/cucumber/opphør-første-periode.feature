# language: no
# encoding: UTF-8

Egenskap: Opphør første periode

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId |
      | 1        |

    Og følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | OPPHØRT             | NYE_OPPLYSNINGER | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 14.07.1983  |
      | 1            | 2       | BARN       | 25.08.2022  |
      | 2            | 1       | SØKER      | 14.07.1983  |
      | 2            | 2       | BARN       | 25.08.2022  |

  Scenario: Når det er opphør første periode, men vi har vilkårresultat før den første, ønsker vi fortsatt å få opphørstekster for første periode
    Og følgende dagens dato 14.02.2024

    Og følgende vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET               |                  | 15.03.2010 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | MEDLEMSKAP                   |                  | 15.03.2015 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER    |                  | 01.12.2008 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 25.08.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNEHAGEPLASS               |                  | 25.08.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BARNETS_ALDER                |                  | 25.08.2023 | 25.08.2024 | OPPFYLT  | Nei                  |                      |                  |

    Og følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                    | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET            |                  | 15.03.2010 | 01.09.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | MEDLEMSKAP                |                  | 15.03.2015 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | MEDLEMSKAP_ANNEN_FORELDER |                  | 01.12.2008 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNEHAGEPLASS            |                  | 25.08.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET            |                  | 25.08.2022 | 01.09.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER             |                  | 25.08.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BARNETS_ALDER             |                  | 25.08.2023 | 25.08.2024 | OPPFYLT  | Nei                  |                      |                  |

    Og andeler er beregnet for behandling 1

    Og andeler er beregnet for behandling 2

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats | Nasjonalt periodebeløp | Differanseberegnet beløp |


    Og vedtaksperioder er laget for behandling 2

    Så forvent følgende vedtaksperioder på behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.09.2023 | 31.07.2024 | OPPHØR             |           |


    Så forvent at følgende begrunnelser er gyldige for behandling 2
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                                                                             | Ugyldige begrunnelser |
      | 01.09.2023 | 31.07.2024 | OPPHØR             |                                | OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE, OPPHØR_IKKE_BOSATT_I_NORGE, OPPHØR_FLYTTET_FRA_NORGE, OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                                                                             | Eøsbegrunnelser | Fritekster |
      | 01.09.2023 | 31.07.2024 | OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE, OPPHØR_IKKE_BOSATT_I_NORGE, OPPHØR_FLYTTET_FRA_NORGE, OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE |                 |            |


    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2023 til 31.07.2024
      | Begrunnelse                          | Type     | Antall barn | Barnas fødselsdatoer | Gjelder søker | Beløp | Måned og år begrunnelsen gjelder for | Gjelder andre forelder |
      | OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   |
      | OPPHØR_IKKE_BOSATT_I_NORGE           | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   |
      | OPPHØR_FLYTTET_FRA_NORGE             | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   |
      | OPPHØR_FRA_START_IKKE_BOSATT_I_NORGE | STANDARD | 1           | 25.08.22             | ja            | 0     | september 2023                       | true                   |