package no.nav.familie.ks.sak.kjerne.brev.domene.maler

data class BrevUtenDataDto(
    override val mal: Brevmal,
    override val data: BrevDataDto,
) : BrevDto {
    constructor(
        mal: Brevmal,
        mottakerNavn: String,
        mottakerIdent: String,
    ) : this(
        mal,
        object : BrevDataDto {
            override val delmalData = null
            override val flettefelter =
                FlettefelterForDokumentDtoImpl(
                    navn = mottakerNavn,
                    fodselsnummer = mottakerIdent,
                )
        },
    )
}
