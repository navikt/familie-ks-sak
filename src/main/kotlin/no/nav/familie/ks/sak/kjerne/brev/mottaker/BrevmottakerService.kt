package no.nav.familie.ks.sak.kjerne.brev.mottaker

import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.api.dto.Bruker
import no.nav.familie.ks.sak.api.dto.BrukerMedUtenlandskAdresse
import no.nav.familie.ks.sak.api.dto.Dødsbo
import no.nav.familie.ks.sak.api.dto.FullmektigEllerVerge
import no.nav.familie.ks.sak.api.dto.ManuellAdresseInfo
import no.nav.familie.ks.sak.api.dto.MottakerInfo
import no.nav.familie.ks.sak.api.dto.tilBrevMottakerDb
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BrevmottakerService(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val loggService: LoggService,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
) {
    @Transactional
    fun leggTilBrevmottaker(
        brevmottakerDto: BrevmottakerDto,
        behandlingId: Long,
    ) {
        val brevmottaker = brevmottakerDto.tilBrevMottakerDb(behandlingId)

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId = behandlingId,
            nyBrevmottaker = brevmottaker,
            ekstraBarnLagtTilIBrev = emptyList(),
        )

        loggService.opprettBrevmottakerLogg(
            brevmottaker = brevmottaker,
            brevmottakerFjernet = false,
        )

        brevmottakerRepository.save(brevmottaker)
    }

    @Transactional
    fun fjernBrevmottaker(id: Long) {
        val brevmottaker =
            brevmottakerRepository.findByIdOrNull(id) ?: throw Feil("Finner ikke brevmottaker med id=$id")

        loggService.opprettBrevmottakerLogg(
            brevmottaker = brevmottaker,
            brevmottakerFjernet = true,
        )

        brevmottakerRepository.deleteById(id)
    }

    fun hentBrevmottakere(behandlingId: Long) =
        brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId).map {
            BrevmottakerDto(
                id = it.id,
                type = it.type,
                navn = it.navn,
                adresselinje1 = it.adresselinje1,
                adresselinje2 = it.adresselinje2,
                postnummer = it.postnummer,
                poststed = it.poststed,
                landkode = it.landkode,
            )
        }

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
