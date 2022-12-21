package no.nav.familie.ks.sak.bisys

import no.nav.familie.ks.sak.api.dto.BisysResponsDto
import no.nav.familie.ks.sak.api.dto.UtbetalingsinfoDto
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BisysService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val personidentService: PersonidentService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val infotrygdReplikaClient: InfotrygdReplikaClient
) {

    fun hentUtbetalingsinfo(barnIdenter: List<String>): BisysResponsDto {
        // hent fagsaker
        val barnAktører = barnIdenter.map { personidentService.hentAktør(it) }
        val fagsaker = barnAktører.mapNotNull { fagsakService.finnFagsakForPerson(it) }

        // hent siste vedtatt behandlinger fra fagsak
        val behandlinger = fagsaker.mapNotNull { behandlingService.hentSisteBehandlingSomErVedtatt(it.id) }

        val utbetalinger = mutableMapOf<String, List<UtbetalingsinfoDto>>()

        // hent utbetalingsinfo from ks-sak for hver behandling
        val utbetalingsinfoFraKsSak = behandlinger.map { behandling ->
            val andeler =
                andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
            andeler.filter { it.kalkulertUtbetalingsbeløp != 0 }.map {
                it.aktør.aktivFødselsnummer() to UtbetalingsinfoDto(
                    fomMåned = it.stønadFom,
                    tomMåned = it.stønadTom,
                    beløp = it.kalkulertUtbetalingsbeløp
                )
            }
        }.flatten()

        // hent utbetalingsinfo from infotrygd
        val respons = infotrygdReplikaClient.hentKontantstøttePerioderFraInfotrygd(barnIdenter)
        val utbetalingsinfoFraInfotrygd = respons.data.map { stonad ->
            val barn = stonad.barn.first { stonadBarn -> barnIdenter.any { it == stonadBarn.fnr.asString } }.fnr.asString
            barn to UtbetalingsinfoDto(
                fomMåned = checkNotNull(stonad.fom),
                tomMåned = checkNotNull(stonad.tom),
                beløp = checkNotNull(stonad.belop)
            )
        }

        (utbetalingsinfoFraKsSak + utbetalingsinfoFraInfotrygd).groupBy { it.first }.forEach { utbetalingsinfoPerBarn ->
            utbetalinger[utbetalingsinfoPerBarn.key] = utbetalingsinfoPerBarn.value.map { it.second }
        }
        return BisysResponsDto(utbetalingsinfo = utbetalinger.toMap())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BisysService::class.java)
    }
}
