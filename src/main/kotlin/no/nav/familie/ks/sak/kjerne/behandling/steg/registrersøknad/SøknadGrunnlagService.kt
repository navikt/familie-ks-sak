package no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SøknadGrunnlagService(
    private val søknadGrunnlagRepository: SøknadGrunnlagRepository,
) {
    @Transactional
    fun lagreOgDeaktiverGammel(søknadGrunnlag: SøknadGrunnlag): SøknadGrunnlag {
        søknadGrunnlagRepository
            .finnAktiv(søknadGrunnlag.behandlingId)
            ?.let { søknadGrunnlagRepository.saveAndFlush(it.also { it.aktiv = false }) }

        return søknadGrunnlagRepository.save(søknadGrunnlag)
    }

    fun finnAktiv(behandlingId: Long): SøknadGrunnlag? = søknadGrunnlagRepository.finnAktiv(behandlingId)

    fun hentAktiv(behandlingId: Long): SøknadGrunnlag = finnAktiv(behandlingId) ?: throw Feil("Fant ikke aktiv søknadsgrunnlag for behandling $behandlingId.")
}
