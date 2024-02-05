package no.nav.familie.ks.sak.kjerne.brev.mottaker

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.api.dto.ManuellAdresseInfo
import no.nav.familie.ks.sak.api.dto.ManuellBrevmottaker
import no.nav.familie.ks.sak.api.dto.MottakerInfo
import no.nav.familie.ks.sak.api.dto.tilBrevMottaker
import no.nav.familie.ks.sak.api.dto.toList
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BrevmottakerService(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val loggService: LoggService,
    private val personidentService: PersonidentService,
    private val personOpplysningerService: PersonOpplysningerService,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
) {

    @Transactional
    fun leggTilBrevmottaker(
        restBrevMottaker: BrevmottakerDto,
        behandlingId: Long,
    ) {
        val brevmottaker = restBrevMottaker.tilBrevMottaker(behandlingId)

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

    fun hentBrevmottakere(behandlingId: Long) = brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId)

    fun hentRestBrevmottakere(behandlingId: Long) =
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
        manueltRegistrerteMottakere: List<ManuellBrevmottaker>,
        søkersident: String,
        søkersnavn: String = hentMottakerNavn(søkersident),
    ): List<MottakerInfo> {
        manueltRegistrerteMottakere.singleOrNull { it.type == MottakerType.DØDSBO }?.let {
            // brev sendes kun til den manuelt registerte dødsboadressen
            return lagMottakerInfoUtenBrukerId(navn = søkersnavn, manuellAdresseInfo = lagManuellAdresseInfo(it)).toList()
        }

        val manuellAdresseUtenlands =
            manueltRegistrerteMottakere.filter { it.type == MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
                .let {
                    it.takeIf { it.size <= 1 }
                        ?: throw FunksjonellFeil("Mottakerfeil: Det er registrert mer enn en utenlandsk adresse tilhørende bruker")
                }.firstNotNullOfOrNull {
                    lagMottakerInfoMedBrukerId(
                        brukerId = søkersident,
                        navn = søkersnavn,
                        manuellAdresseInfo = lagManuellAdresseInfo(it),
                    )
                }

        // brev sendes til brukers (manuelt) registerte adresse (i utlandet)
        val bruker = manuellAdresseUtenlands ?: lagMottakerInfoMedBrukerId(brukerId = søkersident, navn = søkersnavn)

        // ...og evt. til en manuelt registrert verge eller fullmektig i tillegg
        val manuellTilleggsmottaker =
            manueltRegistrerteMottakere.filter { it.type != MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
                .let {
                    it.takeIf { it.size <= 1 }
                        ?: throw FunksjonellFeil(
                            "Mottakerfeil: ${it.first().type.visningsnavn} kan ikke kombineres med ${it.last().type.visningsnavn}"
                        )
                }.firstNotNullOfOrNull {
                    lagMottakerInfoUtenBrukerId(navn = it.navn, manuellAdresseInfo = lagManuellAdresseInfo(it))
                }

        return listOfNotNull(bruker, manuellTilleggsmottaker)
    }

    fun hentMottakerNavn(personIdent: String): String {
        val aktør = personidentService.hentAktør(personIdent)
        return personOpplysningerService.hentPersoninfoEnkel(aktør).let {
            it.navn!!
        }
    }

    private fun lagManuellAdresseInfo(brevmottaker: ManuellBrevmottaker) =
        ManuellAdresseInfo(
            adresselinje1 = brevmottaker.adresselinje1,
            adresselinje2 = brevmottaker.adresselinje2,
            postnummer = brevmottaker.postnummer,
            poststed = brevmottaker.poststed,
            landkode = brevmottaker.landkode,
        )

    private fun lagMottakerInfoUtenBrukerId(
        navn: String,
        manuellAdresseInfo: ManuellAdresseInfo,
    ): MottakerInfo =
        MottakerInfo(
            brukerId = "",
            brukerIdType = null,
            navn = navn,
            manuellAdresseInfo = manuellAdresseInfo,
        )

    private fun lagMottakerInfoMedBrukerId(
        brukerId: String,
        navn: String,
        manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) = MottakerInfo(
        brukerId = brukerId,
        brukerIdType = BrukerIdType.FNR,
        navn = navn,
        manuellAdresseInfo = manuellAdresseInfo,
    )
}
