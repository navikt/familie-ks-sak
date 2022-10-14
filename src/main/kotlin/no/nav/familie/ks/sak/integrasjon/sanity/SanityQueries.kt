package no.nav.familie.ks.sak.integrasjon.sanity

// TODO: Tilpass query for KS
const val hentBegrunnelser =
    "*[_type == \"begrunnelse\" && behandlingstema != \"EØS\" && apiNavn != null && navnISystem != null]{" +
        "apiNavn," +
        "navnISystem," +
        "hjemler," +
        "hjemlerFolketrygdloven," +
        "vilkaar," +
        "rolle," +
        "lovligOppholdTriggere," +
        "bosattIRiketTriggere," +
        "giftPartnerskapTriggere," +
        "borMedSokerTriggere," +
        "ovrigeTriggere," +
        "endretUtbetalingsperiodeTriggere," +
        "endretUtbetalingsperiodeDeltBostedUtbetalingTrigger," +
        "endringsaarsaker," +
        "utvidetBarnetrygdTriggere" +
        "}"

// TODO: Tilpass query for KS
const val hentEØSBegrunnelser =
    "*[_type == \"begrunnelse\" && behandlingstema == \"EØS\" && apiNavn != null && navnISystem != null]{" +
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
