package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.barnehagelister.domene.BarnehageBydel
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
        val barnehagebarn = barnehagebarnRepository.findByIdOrNull(barnehagebarnId) ?: throw Feil("Fant ikke barnehagebarn med id $barnehagebarnId")
        val barnehageOrgNummer = barnehagebarn.barnehageOrgNummer ?: throw Feil("Barnehagebarn med id $barnehagebarnId mangler organisasjonsnummer for barnehagen")
        val bydelOrgNummer = hentBydelOrgNummer(barnehageOrgNummer)

        // Barnehagen finnes ikke i NBR eller har ingen entydig bydel i Oslo. Barnet får ingen bydel og vises ikke ved filtrering på bydel.
        if (bydelOrgNummer == null) {
            logger.warn("Fant ikke bydel for barnehagebarn $barnehagebarnId i barnehage med organisasjonsnummer $barnehageOrgNummer")
            return
        }

        barnehagebarn.bydelOrgNummer = bydelOrgNummer
        barnehagebarnRepository.save(barnehagebarn)
    }

    /**
     * Henter bydelen fra BARNEHAGE_BYDEL. Finnes den ikke der, slås barnehagen opp i NBR og bydelen lagres.
     */
    private fun hentBydelOrgNummer(barnehageOrgNummer: String): String? {
        barnehageBydelRepository.findByIdOrNull(barnehageOrgNummer)?.let { return it.bydelOrgNummer }

        val bydelOrgNummer = nasjonalBarnehageRegisterKlient.hentEnhet(barnehageOrgNummer)?.finnBydelOrgNummer() ?: return null
        barnehageBydelRepository.save(BarnehageBydel(barnehageOrgNummer = barnehageOrgNummer, bydelOrgNummer = bydelOrgNummer))
        return bydelOrgNummer
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BarnehageBydelService::class.java)
    }
}
