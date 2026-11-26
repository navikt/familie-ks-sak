package no.nav.familie.ks.sak.kjerne.brev.mottaker

import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.api.dto.Bruker
import no.nav.familie.ks.sak.api.dto.BrukerMedUtenlandskAdresse
import no.nav.familie.ks.sak.api.dto.Dødsbo
import no.nav.familie.ks.sak.api.dto.FullmektigEllerVerge
import no.nav.familie.ks.sak.api.dto.ManuellAdresseInfo
import no.nav.familie.ks.sak.api.dto.MottakerInfo
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import org.springframework.stereotype.Service

@Service
class BrevmottakerService(
    private val brevmottakerHenter: BrevmottakerHenter,
    private val brevmottakerOppretter: BrevmottakerOppretter,
    private val brevmottakerSletter: BrevmottakerSletter,
) {
    fun opprettBrevmottaker(brevmottaker: BrevmottakerDb): BrevmottakerDb = brevmottakerOppretter.opprettBrevmottaker(brevmottaker)

    fun slettBrevmottaker(
        id: Long,
        behandlingId: Long,
    ): Unit = brevmottakerSletter.slettBrevmottaker(id = id, behandlingId = behandlingId)

    fun hentBrevmottakere(behandlingId: Long): List<BrevmottakerDb> = brevmottakerHenter.hentBrevmottakere(behandlingId)

    fun lagMottakereFraBrevMottakere(
        manueltRegistrerteMottakere: List<BrevmottakerDto>,
    ): List<MottakerInfo> =
        when {
            manueltRegistrerteMottakere.isEmpty() -> {
                listOf(Bruker())
            }

            manueltRegistrerteMottakere.any { it.type == MottakerType.DØDSBO } -> {
                val dodsbo = manueltRegistrerteMottakere.single { it.type == MottakerType.DØDSBO }
                listOf(Dødsbo(navn = dodsbo.navn, manuellAdresseInfo = lagManuellAdresseInfo(dodsbo)))
            }

            else -> {
                val brukerMedUtenlandskAdresseListe =
                    manueltRegistrerteMottakere
                        .filter { it.type == MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
                        .map { BrukerMedUtenlandskAdresse(lagManuellAdresseInfo(it)) }
                if (brukerMedUtenlandskAdresseListe.size > 1) {
                    throw FunksjonellFeil("Mottakerfeil: Det er registrert mer enn en utenlandsk adresse tilhørende bruker")
                }
                val bruker = brukerMedUtenlandskAdresseListe.firstOrNull() ?: Bruker()

                val tilleggsmottakerListe =
                    manueltRegistrerteMottakere.filter { it.type != MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
                if (tilleggsmottakerListe.size > 1) {
                    throw FunksjonellFeil("Mottakerfeil: ${tilleggsmottakerListe.first().type.visningsnavn} kan ikke kombineres med ${tilleggsmottakerListe.last().type.visningsnavn}")
                }
                val tilleggsmottaker =
                    tilleggsmottakerListe.firstOrNull()?.let {
                        FullmektigEllerVerge(navn = it.navn, manuellAdresseInfo = lagManuellAdresseInfo(it))
                    }
                listOfNotNull(bruker, tilleggsmottaker)
            }
        }

    private fun lagManuellAdresseInfo(brevmottaker: BrevmottakerDto) =
        ManuellAdresseInfo(
            adresselinje1 = brevmottaker.adresselinje1,
            adresselinje2 = brevmottaker.adresselinje2,
            postnummer = brevmottaker.postnummer,
            poststed = brevmottaker.poststed,
            landkode = brevmottaker.landkode,
        )
}
