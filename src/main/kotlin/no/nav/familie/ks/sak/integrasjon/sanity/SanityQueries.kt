package no.nav.familie.ks.sak.integrasjon.sanity

const val hentBegrunnelser =
    "*[_type == \"ksBegrunnelse\" && tema != \"EØS_PRIMÆRLAND\" && tema != \"EØS_SEKUNDÆRLAND\" && apiNavn != null && navnISystem != null]{" +
        "apiNavn," +
        "navnISystem," +
        "hjemler," +
        "hjemlerFolketrygdloven," +
        "vilkaar," +
        "rolle," +
        "lovligOppholdTriggere," +
        "bosattIRiketTriggere," +
        "borMedSokerTriggere," +
        "ovrigeTriggere," +
        "endretUtbetalingsperiodeTriggere," +
        "endretUtbetalingsperiodeDeltBostedUtbetalingTrigger," +
        "endringsaarsaker," +
        "}"

const val hentEØSBegrunnelser =
    "*[_type == \"ksBegrunnelse\" && tema == \"EØS_PRIMÆRLAND\" && tema == \"EØS_SEKUNDÆRLAND\" && apiNavn != null && navnISystem != null]{" +
        "apiNavn," +
        "navnISystem," +
        "hjemler," +
        "hjemlerFolketrygdloven," +
        "hjemlerEOSForordningen883," +
        "hjemlerEOSForordningen987," +
        "hjemlerSeperasjonsavtalenStorbritannina," +
        "annenForeldersAktivitet," +
        "barnetsBostedsland," +
        "kompetanseResultat" +
        "}"
