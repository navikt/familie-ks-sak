package no.nav.familie.ks.sak.api.mapper

import no.nav.familie.ks.sak.api.dto.SimuleringDto
import no.nav.familie.ks.sak.api.dto.SimuleringsPeriodeDto
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.filterBortIrrelevanteVedtakSimuleringPosteringer
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.hentEtterbetalingIPeriode
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.hentNyttBeløpIPeriode
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.hentPositivFeilbetalingIPeriode
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.hentResultatIPeriode
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.hentTidligereUtbetaltIPeriode
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.hentTotalEtterbetaling
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.hentTotalFeilutbetaling
import java.math.BigDecimal
import java.time.LocalDate

object SimuleringMapper {

    fun List<ØkonomiSimuleringMottaker>.tilSimuleringDto(): SimuleringDto {
        val perioder = this.tilSimuleringsPerioder()
        val tidSimuleringHentet = this.firstOrNull()?.opprettetTidspunkt?.toLocalDate()

        val framtidigePerioder =
            perioder.filter {
                it.fom > tidSimuleringHentet ||
                    (it.tom > tidSimuleringHentet && it.forfallsdato > tidSimuleringHentet)
            }

        val nestePeriode = framtidigePerioder.filter { it.feilutbetaling == BigDecimal.ZERO }.minByOrNull { it.fom }
        val tomSisteUtbetaling =
            perioder.filter { nestePeriode == null || it.fom < nestePeriode.fom }.maxOfOrNull { it.tom }

        return SimuleringDto(
            perioder = perioder,
            fomDatoNestePeriode = nestePeriode?.fom,
            etterbetaling = hentTotalEtterbetaling(perioder, nestePeriode?.fom),
            feilutbetaling = hentTotalFeilutbetaling(perioder, nestePeriode?.fom)
                .let { if (it < BigDecimal.ZERO) BigDecimal.ZERO else it },
            fom = perioder.minOfOrNull { it.fom },
            tomDatoNestePeriode = nestePeriode?.tom,
            forfallsdatoNestePeriode = nestePeriode?.forfallsdato,
            tidSimuleringHentet = tidSimuleringHentet,
            tomSisteUtbetaling = tomSisteUtbetaling
        )
    }

    fun List<ØkonomiSimuleringMottaker>.tilSimuleringsPerioder(): List<SimuleringsPeriodeDto> {
        val simuleringPerioder = mutableMapOf<LocalDate, MutableList<ØkonomiSimuleringPostering>>()

        filterBortIrrelevanteVedtakSimuleringPosteringer(this).forEach {
            it.økonomiSimuleringPostering.forEach { postering ->
                if (simuleringPerioder.containsKey(postering.fom)) {
                    simuleringPerioder[postering.fom]?.add(postering)
                } else {
                    simuleringPerioder[postering.fom] = mutableListOf(postering)
                }
            }
        }

        val tidSimuleringHentet = this.firstOrNull()?.opprettetTidspunkt?.toLocalDate()

        return simuleringPerioder.map { (fom, posteringListe) ->
            SimuleringsPeriodeDto(
                fom,
                posteringListe[0].tom,
                posteringListe[0].forfallsdato,
                nyttBeløp = hentNyttBeløpIPeriode(posteringListe),
                tidligereUtbetalt = hentTidligereUtbetaltIPeriode(posteringListe),
                resultat = hentResultatIPeriode(posteringListe),
                feilutbetaling = hentPositivFeilbetalingIPeriode(posteringListe),
                etterbetaling = hentEtterbetalingIPeriode(posteringListe, tidSimuleringHentet)
            )
        }
    }
}
