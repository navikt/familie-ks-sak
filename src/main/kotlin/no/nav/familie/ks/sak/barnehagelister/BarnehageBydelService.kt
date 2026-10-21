package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.barnehagelister.domene.BarnehageBydelRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.nbr.NasjonalBarnehageRegisterKlient
import no.nav.familie.ks.sak.integrasjon.nbr.domene.finnBydelOrgNummer
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BarnehageBydelService(
    private val barnehagebarnRepository: BarnehagebarnRepository,
    private val barnehageBydelRepository: BarnehageBydelRepository,
    private val nasjonalBarnehageRegisterKlient: NasjonalBarnehageRegisterKlient,
) {
    @Transactional
    fun oppdaterBydelOrgNummerPåBarnehagebarn(barnehagebarnId: UUID) {
        val barnehagebarn =
            barnehagebarnRepository.findByIdOrNull(barnehagebarnId)
                ?: throw Feil("Fant ikke barnehagebarn med id $barnehagebarnId")
        val barnehageOrgNummer =
            barnehagebarn.barnehageOrgNummer
                ?: throw Feil("Barnehagebarn med id $barnehagebarnId mangler barnehage org nummer")

        val bydelOrgNummer = hentBydelOrgNummer(barnehageOrgNummer)
        if (bydelOrgNummer == null) {
            logger.warn("Fant ikke bydel orgnummer for barnehagebarn $barnehagebarnId med barnehage org nummer $barnehageOrgNummer")
            return
        }

        barnehagebarn.bydelOrgNummer = bydelOrgNummer
        barnehagebarnRepository.save(barnehagebarn)
    }

    /**
     * Henter bydelen fra BARNEHAGE_BYDEL. Finnes den ikke der, slås barnehagen opp i NBR og bydelen lagres.
     */
    private fun hentBydelOrgNummer(barnehageOrgNummer: String): String? {
        barnehageBydelRepository.findByBarnehageOrgNummer(barnehageOrgNummer)?.let { return it.bydelOrgNummer }

        val bydelOrgNummer = nasjonalBarnehageRegisterKlient.hentEnhet(barnehageOrgNummer)?.finnBydelOrgNummer() ?: return null
        barnehageBydelRepository.lagreHvisIkkeFinnes(barnehageOrgNummer = barnehageOrgNummer, bydelOrgNummer = bydelOrgNummer)
        return bydelOrgNummer
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BarnehageBydelService::class.java)
    }
}
