package no.nav.familie.ks.sak.kjerne.behandling.steg.søknad

import no.nav.familie.ks.sak.kjerne.behandling.steg.søknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.behandling.steg.søknad.domene.SøknadGrunnlagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SøknadGrunnlagService(
    private val søknadGrunnlagRepository: SøknadGrunnlagRepository
) {
    @Transactional
    fun lagreOgDeaktiverGammel(søknadGrunnlag: SøknadGrunnlag): SøknadGrunnlag {
        søknadGrunnlagRepository.finnAktiv(søknadGrunnlag.behandlingId)
            ?.let { søknadGrunnlagRepository.saveAndFlush(it.also { it.aktiv = false }) }

        return søknadGrunnlagRepository.save(søknadGrunnlag)
    }

    fun finnAktiv(behandlingId: Long): SøknadGrunnlag? {
        return søknadGrunnlagRepository.finnAktiv(behandlingId)
    }
}
