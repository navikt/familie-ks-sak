# language: no
# encoding: UTF-8

Egenskap: Utbetalingsoppdrag: Håndtering av 0-beløp


  Scenario: Endrer en tidligere periode til 0-utbetaling

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 1            | 04.2021  | 04.2021  | 700   |
      | 2            | 03.2021  | 03.2021  | 0     |
      | 2            | 04.2021  | 04.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 1            | 04.2021  | 04.2021  |             | 700   | NY           | Nei        | 1          | 0                  | 1               |

      | 2            | 04.2021  | 04.2021  | 03.2021     | 700   | ENDR         | Ja         | 1          | 0                  | 2               |
      | 2            | 04.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  | 2               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |
      | 1            | 1  | 1          | 0                  | 1               |

      | 2            | 3  | 2          | 1                  | 2               |

  Scenario: Splitter en periode til 2 perioder der en av de får 0-beløp

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 04.2021  | 700   |
      | 1            | 05.2021  | 06.2021  | 800   |

      | 2            | 03.2021  | 03.2021  | 700   |
      | 2            | 04.2021  | 04.2021  | 0     |
      | 2            | 05.2021  | 06.2021  | 800   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 04.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 1            | 05.2021  | 06.2021  |             | 800   | NY           | Nei        | 1          | 0                  | 1               |

      | 2            | 05.2021  | 06.2021  | 04.2021     | 800   | ENDR         | Ja         | 1          | 0                  | 2               |
      | 2            | 05.2021  | 06.2021  |             | 800   | ENDR         | Nei        | 2          | 1                  | 2               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |
      | 1            | 1  | 1          | 0                  | 1               |

      | 2            | 2  | 0          |                    | 1               |
      | 2            | 4  | 2          | 1                  | 2               |

  Scenario: 0 beløp før forrige periode skal opphøra bak i tiden, for å kunne opphøre bak i tiden til når infotrygd eide dataen

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |

      | 2            | 02.2021  | 02.2021  | 0     |
      | 2            | 03.2021  | 03.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |

      | 2            | 03.2021  | 03.2021  | 02.2021     | 700   | ENDR         | Ja         | 0          |                    | 2               |
      | 2            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  | 2               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |

      | 2            | 2  | 1          | 0                  | 2               |

  Scenario: 0-beløp beholdes, og får en ny andel

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 1            | 04.2021  | 04.2021  | 0     |
      | 1            | 05.2021  | 05.2021  | 800   |

      | 2            | 03.2021  | 03.2021  | 700   |
      | 2            | 04.2021  | 04.2021  | 0     |
      | 2            | 05.2021  | 05.2021  | 800   |
      | 2            | 06.2021  | 06.2021  | 900   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 1            | 05.2021  | 05.2021  |             | 800   | NY           | Nei        | 1          | 0                  | 1               |

      | 2            | 06.2021  | 06.2021  |             | 900   | ENDR         | Nei        | 2          | 1                  | 2               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |
      | 1            | 2  | 1          | 0                  | 1               |

      | 2            | 3  | 0          |                    | 1               |
      | 2            | 5  | 1          | 0                  | 1               |
      | 2            | 6  | 2          | 1                  | 2               |