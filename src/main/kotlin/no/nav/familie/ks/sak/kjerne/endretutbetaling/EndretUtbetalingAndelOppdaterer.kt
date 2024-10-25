package no.nav.familie.ks.sak.kjerne.endretutbetaling

import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelRequestDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EndretUtbetalingAndelOppdaterer(
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
) {
    @Transactional
    fun oppdaterEndretUtbetalingAndel(
        endretUtbetalingAndelRequestDto: EndretUtbetalingAndelRequestDto,
        personPåEndretUtbetalingsandel: Person,
    ): EndretUtbetalingAndel {
        val endretUtbetalingAndel = endretUtbetalingAndelRepository.getReferenceById(endretUtbetalingAndelRequestDto.id)
        return endretUtbetalingAndel.apply {
            fom = endretUtbetalingAndelRequestDto.fom
            tom = endretUtbetalingAndelRequestDto.tom
            prosent = endretUtbetalingAndelRequestDto.prosent
            årsak = endretUtbetalingAndelRequestDto.årsak
            avtaletidspunktDeltBosted = endretUtbetalingAndelRequestDto.avtaletidspunktDeltBosted
            søknadstidspunkt = endretUtbetalingAndelRequestDto.søknadstidspunkt
            begrunnelse = endretUtbetalingAndelRequestDto.begrunnelse
            person = personPåEndretUtbetalingsandel
            erEksplisittAvslagPåSøknad = endretUtbetalingAndelRequestDto.erEksplisittAvslagPåSøknad
            begrunnelser = finnBegrunnelse(endretUtbetalingAndelRequestDto)
        }
    }

    private fun finnBegrunnelse(endretUtbetalingAndelRequestDto: EndretUtbetalingAndelRequestDto): List<NasjonalEllerFellesBegrunnelse> {
        if (endretUtbetalingAndelRequestDto.erEksplisittAvslagPåSøknad == false) {
            return emptyList()
        }
        return when (endretUtbetalingAndelRequestDto.årsak) {
            Årsak.DELT_BOSTED,
            Årsak.ENDRE_MOTTAKER,
            Årsak.ALLEREDE_UTBETALT,
            Årsak.ETTERBETALING_3MND,
            -> listOf(NasjonalEllerFellesBegrunnelse.AVSLAG_SØKT_FOR_SENT_ENDRINGSPERIODE)

            Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
            -> listOf(NasjonalEllerFellesBegrunnelse.AVSLAG_BARNEHAGEPLASS_AUGUST_2024)
        }
    }
}
