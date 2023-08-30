package no.nav.familie.ks.sak.kjerne.brev.domene.maler

data class HenleggeTrukketSøknadBrevDto(
    override val mal: Brevmal = Brevmal.HENLEGGE_TRUKKET_SØKNAD,
    override val data: HenleggeTrukketSøknadDataDto,
) : BrevDto

data class HenleggeTrukketSøknadDataDto(
    override val delmalData: DelmalData,
    override val flettefelter: FlettefelterForDokumentDtoImpl,
) : BrevDataDto {

    data class DelmalData(
        val signatur: SignaturDelmal,
    )
}
