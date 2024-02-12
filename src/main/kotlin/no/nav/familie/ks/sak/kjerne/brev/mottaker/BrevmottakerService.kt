package no.nav.familie.ks.sak.kjerne.brev.mottaker

import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.api.dto.Bruker
import no.nav.familie.ks.sak.api.dto.BrukerMedUtenlandskAdresse
import no.nav.familie.ks.sak.api.dto.Dødsbo
import no.nav.familie.ks.sak.api.dto.FullmektigEllerVerge
import no.nav.familie.ks.sak.api.dto.ManuellAdresseInfo
import no.nav.familie.ks.sak.api.dto.MottakerInfo
import no.nav.familie.ks.sak.api.dto.tilBrevMottakerDb
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
    ): List<MottakerInfo> {
        if (manueltRegistrerteMottakere.isEmpty()) {
            return Bruker().toList()
        }

        manueltRegistrerteMottakere.singleOrNull { it.type == MottakerType.DØDSBO }?.let {
            // brev sendes kun til den manuelt registerte dødsboadressen
            return Dødsbo(navn = it.navn, manuellAdresseInfo = lagManuellAdresseInfo(it)).toList()
        }

        val manuellAdresseUtenlands =
            manueltRegistrerteMottakere.filter { it.type == MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
                .let {
                    it.takeIf { it.size <= 1 }
                        ?: throw FunksjonellFeil("Mottakerfeil: Det er registrert mer enn en utenlandsk adresse tilhørende bruker")
                }.firstNotNullOfOrNull {
                    BrukerMedUtenlandskAdresse(
                        manuellAdresseInfo = lagManuellAdresseInfo(it),
                    )
                }

        // brev sendes til brukers (manuelt) registerte adresse (i utlandet)
        val bruker = manuellAdresseUtenlands ?: Bruker()

        // ...og evt. til en manuelt registrert verge eller fullmektig i tillegg
        val manuellTilleggsmottaker =
            manueltRegistrerteMottakere.filter { it.type != MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
                .let {
                    it.takeIf { it.size <= 1 }
                        ?: throw FunksjonellFeil(
                            "Mottakerfeil: ${it.first().type.visningsnavn} kan ikke kombineres med ${it.last().type.visningsnavn}",
                        )
                }.firstNotNullOfOrNull {
                    FullmektigEllerVerge(
                        navn = it.navn,
                        manuellAdresseInfo = lagManuellAdresseInfo(it),
                    )
                }

        return listOfNotNull(bruker, manuellTilleggsmottaker)
    }

    fun hentMottakerNavn(personIdent: String): String {
        val aktør = personidentService.hentAktør(personIdent)
        return personOpplysningerService.hentPersoninfoEnkel(aktør).let {
            it.navn!!
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
