package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersonopplysningGrunnlagLagreService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) {
    @Transactional
    fun lagreOgSlettGammelt(personopplysningGrunnlag: PersonopplysningGrunnlag): PersonopplysningGrunnlag {
        val aktivtPersonopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(personopplysningGrunnlag.behandlingId)

        if (aktivtPersonopplysningGrunnlag != null) {
            personopplysningGrunnlagRepository.delete(aktivtPersonopplysningGrunnlag)
            personopplysningGrunnlagRepository.flush()
        }

        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter persongrunnlag for behandling ${personopplysningGrunnlag.behandlingId}")
        return personopplysningGrunnlagRepository.saveAndFlush(personopplysningGrunnlag)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PersonopplysningGrunnlagLagreService::class.java)
    }
}
