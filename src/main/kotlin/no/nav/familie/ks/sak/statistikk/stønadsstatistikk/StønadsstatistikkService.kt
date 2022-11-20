package no.nav.familie.ks.sak.statistikk.stønadsstatistikk

import no.nav.familie.eksterne.kontrakter.BehandlingTypeV2
import no.nav.familie.eksterne.kontrakter.BehandlingÅrsakV2
import no.nav.familie.eksterne.kontrakter.KategoriV2
import no.nav.familie.eksterne.kontrakter.PersonDVHV2
import no.nav.familie.eksterne.kontrakter.UnderkategoriV2
import no.nav.familie.eksterne.kontrakter.UtbetalingsDetaljDVHV2
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVHV2
import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.lagVertikalePerioder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap.filtrerGjeldendeNå
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.util.UUID

@Service
class StønadsstatistikkService(
    private val behandlingService: BehandlingService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val personOpplysningerService: PersonOpplysningerService,
    private val vedtakService: VedtakService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {

    fun hentVedtakV2(behandlingId: Long): VedtakDVHV2 {
        val behandling = behandlingService.hentBehandling(behandlingId)

        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId)
        val datoVedtak = vedtak.vedtaksdato ?: error("Fant ikke vedtaksdato for behandling $behandlingId")

        val tidspunktVedtak = datoVedtak

        return VedtakDVHV2(
            fagsakId = behandling.fagsak.id.toString(),
            behandlingsId = behandlingId.toString(),
            tidspunktVedtak = tidspunktVedtak.atZone(TIMEZONE),
            personV2 = hentSøkerV2(behandlingId),
            ensligForsørger = false, // Bruk VedtakDVHV2 fra ks og fjern dette når branch er merget
            kategoriV2 = KategoriV2.valueOf(behandling.kategori.name),
            underkategoriV2 = UnderkategoriV2.ORDINÆR,
            behandlingTypeV2 = BehandlingTypeV2.valueOf(behandling.type.name),
            utbetalingsperioderV2 = hentUtbetalingsperioderV2(behandlingId),
            funksjonellId = UUID.randomUUID().toString(),
            behandlingÅrsakV2 = BehandlingÅrsakV2.valueOf(behandling.opprettetÅrsak.name)
        )
    }

    private fun hentSøkerV2(behandlingId: Long): PersonDVHV2 {
        val persongrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)

        return lagPersonDVHV2(persongrunnlag.søker)
    }

    private fun hentUtbetalingsperioderV2(behandlingId: Long): List<UtbetalingsperiodeDVHV2> {
        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandlingId
            )
        val behandling = behandlingService.hentBehandling(behandlingId)
        val persongrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)

        if (andelerTilkjentYtelse.isEmpty()) return emptyList()

        val utbetalingsPeriodeDetaljer = andelerTilkjentYtelse.lagVertikalePerioder().tilPerioderIkkeNull()

        return utbetalingsPeriodeDetaljer.map {
            val andelerForPeriode = it.verdi
            val sumUtbetalingsbeløp = andelerForPeriode.sumOf { andel -> andel.kalkulertUtbetalingsbeløp }

            UtbetalingsperiodeDVHV2(
                hjemmel = "Ikke implementert",
                stønadFom = it.fom!!,
                stønadTom = it.tom!!,
                utbetaltPerMnd = sumUtbetalingsbeløp,
                utbetalingsDetaljer = andelerForPeriode.filter { andel -> andel.erAndelSomSkalSendesTilOppdrag() }
                    .map { andel ->
                        UtbetalingsDetaljDVHV2(
                            person = lagPersonDVHV2(
                                persongrunnlag.personer.first { person -> andel.aktør == person.aktør },
                                andel.prosent.intValueExact()
                            ),
                            klassekode = andel.type.klassifisering,
                            utbetaltPrMnd = andel.kalkulertUtbetalingsbeløp,
                            delytelseId = behandling.fagsak.id.toString() + andel.periodeOffset
                        )
                    }
            )
        }
    }

    private fun lagPersonDVHV2(person: Person, delingsProsentYtelse: Int = 0): PersonDVHV2 {
        return PersonDVHV2(
            rolle = person.type.name,
            statsborgerskap = hentStatsborgerskap(person),
            bostedsland = hentLandkode(person),
            delingsprosentYtelse = if (delingsProsentYtelse == 50) delingsProsentYtelse else 0,
            personIdent = person.aktør.aktivFødselsnummer()
        )
    }

    private fun hentStatsborgerskap(person: Person): List<String> {
        return if (person.statsborgerskap.isNotEmpty()) {
            person.statsborgerskap.filtrerGjeldendeNå().map { it.landkode }
        } else {
            listOf(personOpplysningerService.hentGjeldendeStatsborgerskap(person.aktør).land)
        }
    }

    private fun hentLandkode(person: Person): String =
        when {
            person.bostedsadresser.isNotEmpty() ||
                personOpplysningerService.hentPersoninfoEnkel(person.aktør).bostedsadresser.isNotEmpty()
            -> "NO"

            else -> personOpplysningerService.hentLandkodeUtenlandskBostedsadresse(person.aktør)
        }

    companion object {
        private val TIMEZONE = ZoneId.of("Europe/Paris")
    }
}
