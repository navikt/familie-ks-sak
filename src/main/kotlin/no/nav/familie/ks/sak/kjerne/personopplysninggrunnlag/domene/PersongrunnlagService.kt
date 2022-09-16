package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PersongrunnlagService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) {

    fun hentAktiv(behandlingId: Long): PersonopplysningGrunnlag? =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)

    companion object {
        private val logger = LoggerFactory.getLogger(PersongrunnlagService::class.java)
    }
}
