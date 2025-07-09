package no.nav.familie.ks.sak.statistikk.stønadsstatistikk

import no.nav.familie.eksterne.kontrakter.BehandlingType
import no.nav.familie.eksterne.kontrakter.BehandlingÅrsak
import no.nav.familie.eksterne.kontrakter.Kategori
import no.nav.familie.eksterne.kontrakter.KompetanseAktivitet
import no.nav.familie.eksterne.kontrakter.KompetanseResultat
import no.nav.familie.eksterne.kontrakter.PersonDVH
import no.nav.familie.eksterne.kontrakter.UtbetalingsDetaljDVH
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVH
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.pdl.PersonopplysningerService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.lagVertikalePerioder
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap.filtrerGjeldendeNå
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.util.UUID

@Service
class StønadsstatistikkService(
    private val behandlingService: BehandlingService,
    private val kompetanseService: KompetanseService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val personOpplysningerService: PersonopplysningerService,
    private val vedtakService: VedtakService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val vilkårsvurderingService: VilkårsvurderingService,
) {
    fun hentVedtakDVH(behandlingId: Long): VedtakDVH {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId)
        val vedtaksdato = vedtak.vedtaksdato ?: throw Feil("Fant ikke vedtaksdato for behandling $behandlingId")
        val aktørId = behandling.fagsak.aktør.aktørId
        val barna = personopplysningGrunnlagService.hentBarna(behandling.id)

        val kompetanser = kompetanseService.hentKompetanser(BehandlingId(behandling.id))
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId)
        val vilkårResultaterForSøker = vilkårsvurdering.hentPersonResultaterTilAktør(aktørId)
        val vilkårResultaterForAlleBarn = barna?.map { vilkårsvurdering.hentPersonResultaterTilAktør(it.aktør.aktørId) }?.flatten()
        val alleVilkårResultater = vilkårResultaterForSøker + vilkårResultaterForAlleBarn.orEmpty()
        val vilkårResultaterTilDVH =
            alleVilkårResultater.map { vkr ->
                no.nav.familie.eksterne.kontrakter.VilkårResultat(
                    resultat = vkr.resultat.tilDatavarehusResultat(),
                    antallTimer = vkr.antallTimer,
                    periodeFom = vkr.periodeFom,
                    periodeTom = vkr.periodeTom,
                    ident = vkr.personResultat?.aktør?.aktivFødselsnummer(),
                    vilkårType = vkr.vilkårType.tilDatavarehusVilkårType(),
                )
            }

        return VedtakDVH(
            fagsakId = behandling.fagsak.id.toString(),
            behandlingsId = behandlingId.toString(),
            tidspunktVedtak = vedtaksdato.atZone(TIMEZONE),
            person = hentSøker(behandlingId),
            kategori = Kategori.valueOf(behandling.kategori.name),
            kompetanseperioder = kompetanser.tilEksernkontraktKompetanser(),
            behandlingType = BehandlingType.valueOf(behandling.type.name),
            utbetalingsperioder = hentUtbetalingsperioder(behandlingId),
            funksjonellId = UUID.randomUUID().toString(),
            behandlingÅrsak = BehandlingÅrsak.valueOf(behandling.opprettetÅrsak.name),
            vilkårResultater = vilkårResultaterTilDVH,
        )
    }

    fun Resultat.tilDatavarehusResultat(): no.nav.familie.eksterne.kontrakter.Resultat =
        no.nav.familie.eksterne.kontrakter.Resultat
            .valueOf(this.name)

    fun Vilkår.tilDatavarehusVilkårType(): no.nav.familie.eksterne.kontrakter.Vilkår =
        no.nav.familie.eksterne.kontrakter.Vilkår
            .valueOf(this.name)

    fun List<Kompetanse>.tilEksernkontraktKompetanser(): List<no.nav.familie.eksterne.kontrakter.Kompetanse> =
        this.filter { it.resultat != null }.map { kompetanse ->
            no.nav.familie.eksterne.kontrakter.Kompetanse(
                barnsIdenter = kompetanse.barnAktører.map { aktør -> aktør.aktivFødselsnummer() },
                annenForeldersAktivitet = kompetanse.annenForeldersAktivitet?.let { KompetanseAktivitet.valueOf(it.name) },
                annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland,
                barnetsBostedsland = kompetanse.barnetsBostedsland,
                fom = kompetanse.fom!!,
                tom = kompetanse.tom,
                resultat = KompetanseResultat.valueOf(kompetanse.resultat!!.name),
                kompetanseAktivitet = if (kompetanse.søkersAktivitet != null) KompetanseAktivitet.valueOf(kompetanse.søkersAktivitet.name) else null,
                sokersAktivitetsland = kompetanse.søkersAktivitetsland,
            )
        }

    private fun hentSøker(behandlingId: Long): PersonDVH {
        val persongrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)

        return lagPersonDVH(persongrunnlag.søker)
    }

    private fun hentUtbetalingsperioder(behandlingId: Long): List<UtbetalingsperiodeDVH> {
        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                behandlingId,
            )
        val behandling = behandlingService.hentBehandling(behandlingId)
        val persongrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId)

        if (andelerTilkjentYtelse.isEmpty()) return emptyList()

        val utbetalingsPeriodeDetaljer = andelerTilkjentYtelse.lagVertikalePerioder().tilPerioderIkkeNull()

        return utbetalingsPeriodeDetaljer.map {
            val andelerForPeriode = it.verdi
            val sumUtbetalingsbeløp = andelerForPeriode.sumOf { andel -> andel.kalkulertUtbetalingsbeløp }

            UtbetalingsperiodeDVH(
                hjemmel = "Ikke implementert",
                stønadFom = it.fom!!,
                stønadTom = it.tom!!,
                utbetaltPerMnd = sumUtbetalingsbeløp,
                utbetalingsDetaljer =
                    andelerForPeriode
                        .filter { it.kalkulertUtbetalingsbeløp != 0 }
                        .map { andel ->
                            UtbetalingsDetaljDVH(
                                person =
                                    lagPersonDVH(
                                        persongrunnlag.personer.first { person -> andel.aktør == person.aktør },
                                        andel.prosent.intValueExact(),
                                    ),
                                klassekode = andel.type.klassifisering,
                                utbetaltPrMnd = andel.kalkulertUtbetalingsbeløp,
                                delytelseId = behandling.fagsak.id.toString() + andel.periodeOffset,
                            )
                        },
            )
        }
    }

    private fun lagPersonDVH(
        person: Person,
        delingsProsentYtelse: Int = 0,
    ): PersonDVH =
        PersonDVH(
            rolle = person.type.name,
            statsborgerskap = hentStatsborgerskap(person),
            bostedsland = hentLandkode(person),
            delingsprosentYtelse = if (delingsProsentYtelse == 50) delingsProsentYtelse else 0,
            personIdent = person.aktør.aktivFødselsnummer(),
        )

    private fun hentStatsborgerskap(person: Person): List<String> =
        if (person.statsborgerskap.isNotEmpty()) {
            person.statsborgerskap.filtrerGjeldendeNå().map { it.landkode }
        } else {
            listOf(personOpplysningerService.hentGjeldendeStatsborgerskap(person.aktør).land)
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
