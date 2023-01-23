package no.nav.familie.ks.sak.bisys

import no.nav.familie.ks.sak.api.dto.Barn
import no.nav.familie.ks.sak.api.dto.BisysResponsDto
import no.nav.familie.ks.sak.api.dto.InfotrygdPeriode
import no.nav.familie.ks.sak.api.dto.KsSakPeriode
import no.nav.familie.ks.sak.common.util.erSammeEllerFør
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BisysService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val personidentService: PersonidentService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val infotrygdReplikaClient: InfotrygdReplikaClient
) {

    fun hentUtbetalingsinfo(fom: LocalDate, identer: List<String>): BisysResponsDto {
        // hent fagsaker
        val aktører = identer.map { personidentService.hentAktør(it) }
        val fagsaker = aktører.map { fagsakService.hentFagsakerPåPerson(it) }.flatten()

        // hent siste vedtatt behandlinger fra fagsak
        val behandlinger = fagsaker.mapNotNull { behandlingService.hentSisteBehandlingSomErVedtatt(it.id) }

        // hent utbetalingsinfo from ks-sak for hver behandling
        val utbetalingsinfoFraKsSak = behandlinger.map { behandling ->
            val andeler =
                andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
            andeler.filter { it.kalkulertUtbetalingsbeløp != 0 }
                .filter { aty -> fom.erSammeEllerFør(aty.stønadTom.toLocalDate()) }
                .map {
                    KsSakPeriode(
                        fomMåned = it.stønadFom,
                        tomMåned = it.stønadTom,
                        barn = Barn(ident = it.aktør.aktivFødselsnummer(), beløp = it.kalkulertUtbetalingsbeløp)
                    )
                }
        }.flatten()

        // hent utbetalingsinfo from infotrygd
        val respons = infotrygdReplikaClient.hentKontantstøttePerioderFraInfotrygd(identer)
        logger.info("Hentet ${respons.data.size} data fra infotrygd")
        val utbetalingsinfoFraInfotrygd = respons.data.filter { stonad ->
            fom.erSammeEllerFør(stonad.tom?.toLocalDate() ?: LocalDate.MAX) // manglende tom dato i infotrygd er løpende stønad
        }.map { stonad ->
            InfotrygdPeriode(
                fomMåned = checkNotNull(stonad.fom) { "fom kan ikke være null" },
                tomMåned = stonad.tom,
                beløp = checkNotNull(stonad.belop) { "beløp kan ikke være null" },
                barna = stonad.barn.map { it.fnr.asString }
            )
        }

        return BisysResponsDto(infotrygdPerioder = utbetalingsinfoFraInfotrygd, ksSakPerioder = utbetalingsinfoFraKsSak)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BisysService::class.java)
    }
}
