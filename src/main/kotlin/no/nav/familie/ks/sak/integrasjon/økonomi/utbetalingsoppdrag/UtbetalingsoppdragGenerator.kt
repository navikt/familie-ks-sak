package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Fagsystem
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.ordinæreAndeler
import no.nav.familie.ks.sak.kjerne.beregning.domene.overgangsordningAndelerPerAktør
import no.nav.familie.ks.sak.kjerne.beregning.domene.totalKalkulertUtbetalingsbeløpForPeriode
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

const val FAGSYSTEM = "KS"
val OVERGANGSORDNING_UTBETALINGSMÅNED = YearMonth.of(2024, 12)

@Component
class UtbetalingsoppdragGenerator {
    fun lagUtbetalingsoppdrag(
        saksbehandlerId: String,
        vedtak: Vedtak,
        forrigeTilkjentYtelse: TilkjentYtelse?,
        nyTilkjentYtelse: TilkjentYtelse,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        erSimulering: Boolean,
        skalSendeOvergangsordningAndeler: Boolean,
    ): BeregnetUtbetalingsoppdragLongId {
        val sisteAndelPerKjedeMedOvergangsordningAndelTattIHensyn = endreSisteAndelPerKjedeHvisOvergangsordningandel(sisteAndelPerKjede, forrigeTilkjentYtelse)

        return Utbetalingsgenerator().lagUtbetalingsoppdrag(
            behandlingsinformasjon =
                Behandlingsinformasjon(
                    saksbehandlerId = saksbehandlerId,
                    behandlingId = vedtak.behandling.id.toString(),
                    eksternBehandlingId = vedtak.behandling.id,
                    eksternFagsakId = vedtak.behandling.fagsak.id,
                    fagsystem = FagsystemKS.KONTANTSTØTTE,
                    personIdent =
                        vedtak.behandling.fagsak.aktør
                            .aktivFødselsnummer(),
                    vedtaksdato = vedtak.vedtaksdato?.toLocalDate() ?: LocalDate.now(),
                    opphørAlleKjederFra = null,
                    utbetalesTil =
                        vedtak.behandling.fagsak.aktør
                            .aktivFødselsnummer(),
                    // Ved simulering når migreringsdato er endret, skal vi opphøre fra den nye datoen og ikke fra første utbetaling per kjede.
                    opphørKjederFraFørsteUtbetaling = erSimulering,
                ),
            forrigeAndeler = forrigeTilkjentYtelse?.tilAndelDataLongId(skalSendeOvergangsordningAndeler) ?: emptyList(),
            nyeAndeler = nyTilkjentYtelse.tilAndelDataLongId(skalSendeOvergangsordningAndeler),
            sisteAndelPerKjede = sisteAndelPerKjedeMedOvergangsordningAndelTattIHensyn,
        )
    }

    private fun endreSisteAndelPerKjedeHvisOvergangsordningandel(
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        forrigeTilkjentYtelse: TilkjentYtelse?,
    ): Map<IdentOgType, AndelDataLongId> {
        if (forrigeTilkjentYtelse == null || sisteAndelPerKjede.none { it.value.type == YtelseType.OVERGANGSORDNING }) {
            return sisteAndelPerKjede.mapValues { it.value.tilAndelDataLongId() }
        }

        return sisteAndelPerKjede.mapValues { (identOgType, andelTilkjentYtelse) ->
            if (andelTilkjentYtelse.type == YtelseType.OVERGANGSORDNING) {
                val overgangsordningsAndelerForAktørSisteTilkjentYtelse =
                    forrigeTilkjentYtelse
                        .overgangsordningAndelerOgOrdinærForOvergangsordningUtbetalingsmånedTilAndelDataLongId()
                        .filter { it.personIdent == identOgType.ident }
                        // Vi henter ut andeler som har fom/tom i desember siden uavhengig av hva fom/tom er på andelen så blir den justert til desember opp mot oppdrag for overgangsordning
                        .singleOrNull { it.fom == OVERGANGSORDNING_UTBETALINGSMÅNED && it.tom == OVERGANGSORDNING_UTBETALINGSMÅNED }
                        ?: error("Greide ikke å utlede overgangsordning andel fra siste tilkjente ytelse for behandling ${andelTilkjentYtelse.behandlingId}")

                overgangsordningsAndelerForAktørSisteTilkjentYtelse
            } else {
                andelTilkjentYtelse.tilAndelDataLongId()
            }
        }
    }

    private fun AndelTilkjentYtelse.tilAndelDataLongId(): AndelDataLongId =
        AndelDataLongId(
            id = id,
            fom = periode.fom,
            tom = periode.tom,
            beløp = kalkulertUtbetalingsbeløp,
            personIdent = aktør.aktivFødselsnummer(),
            type = type.tilYtelseType(),
            periodeId = periodeOffset,
            forrigePeriodeId = forrigePeriodeOffset,
            kildeBehandlingId = kildeBehandlingId,
        )

    private fun TilkjentYtelse.tilAndelDataLongId(skalSendeOvergangsordningAndeler: Boolean): List<AndelDataLongId> {
        val ordinæreAndeler = this.ordinæreAndelertilAndelDataLongId()
        val overgangsordningAndeler =
            when (skalSendeOvergangsordningAndeler) {
                true -> this.overgangsordningAndelerOgOrdinærForOvergangsordningUtbetalingsmånedTilAndelDataLongId()
                false -> emptyList()
            }
        return ordinæreAndeler + overgangsordningAndeler
    }

    private fun TilkjentYtelse.ordinæreAndelertilAndelDataLongId(): List<AndelDataLongId> =
        this
            .andelerTilkjentYtelse
            .ordinæreAndeler()
            .map { it.tilAndelDataLongId() }

    private fun TilkjentYtelse.overgangsordningAndelerOgOrdinærForOvergangsordningUtbetalingsmånedTilAndelDataLongId(): List<AndelDataLongId> {
        val ordinærAndelerPerBarnIOvergangsordningUtbetalingsmåned =
            ordinæreAndelerPerBarnIOvergangsordningUtbetalingMåned()
        return this
            .andelerTilkjentYtelse
            .overgangsordningAndelerPerAktør()
            .map { (aktør, overgangsordningAndeler) ->
                val førsteAndel = overgangsordningAndeler.minBy { it.stønadFom }
                val kalkulertUtbetalingsbeløp =
                    overgangsordningAndeler.sumOf { it.totalKalkulertUtbetalingsbeløpForPeriode() } +
                        (ordinærAndelerPerBarnIOvergangsordningUtbetalingsmåned[aktør]?.kalkulertUtbetalingsbeløp ?: 0)
                AndelDataLongId(
                    id = førsteAndel.id,
                    fom = OVERGANGSORDNING_UTBETALINGSMÅNED,
                    tom = OVERGANGSORDNING_UTBETALINGSMÅNED,
                    beløp = kalkulertUtbetalingsbeløp,
                    personIdent = aktør.aktivFødselsnummer(),
                    type = førsteAndel.type.tilYtelseType(),
                    periodeId = førsteAndel.periodeOffset,
                    forrigePeriodeId = førsteAndel.forrigePeriodeOffset,
                    kildeBehandlingId = førsteAndel.kildeBehandlingId,
                )
            }
    }

    private fun TilkjentYtelse.ordinæreAndelerPerBarnIOvergangsordningUtbetalingMåned(): Map<Aktør, AndelTilkjentYtelse?> =
        andelerTilkjentYtelse
            .ordinæreAndeler()
            .groupBy { it.aktør }
            .mapValues { (_, andeler) ->
                andeler.find { andel ->
                    andel.periode.overlapperHeltEllerDelvisMed(MånedPeriode(OVERGANGSORDNING_UTBETALINGSMÅNED, OVERGANGSORDNING_UTBETALINGSMÅNED))
                }
            }
}

enum class YtelsetypeKS(
    override val klassifisering: String,
    override val satsType: no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType = no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType.MND,
) : no.nav.familie.felles.utbetalingsgenerator.domain.Ytelsestype {
    ORDINÆR_KONTANTSTØTTE("KS"),
}

enum class FagsystemKS(
    override val kode: String,
    override val gyldigeSatstyper: Set<YtelsetypeKS>,
) : Fagsystem {
    KONTANTSTØTTE(
        "KS",
        setOf(YtelsetypeKS.ORDINÆR_KONTANTSTØTTE),
    ),
}

fun no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.tilRestUtbetalingsoppdrag(): Utbetalingsoppdrag =
    Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.valueOf(this.kodeEndring.name),
        fagSystem = this.fagSystem,
        saksnummer = this.saksnummer,
        aktoer = this.aktoer,
        saksbehandlerId = this.saksbehandlerId,
        avstemmingTidspunkt = this.avstemmingTidspunkt,
        utbetalingsperiode = this.utbetalingsperiode.map { it.tilRestUtbetalingsperiode() },
        gOmregning = this.gOmregning,
    )

fun no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.tilRestUtbetalingsperiode(): Utbetalingsperiode =
    Utbetalingsperiode(
        erEndringPåEksisterendePeriode = this.erEndringPåEksisterendePeriode,
        opphør = this.opphør?.let { Opphør(it.opphørDatoFom) },
        periodeId = this.periodeId,
        forrigePeriodeId = this.forrigePeriodeId,
        datoForVedtak = this.datoForVedtak,
        klassifisering = this.klassifisering,
        vedtakdatoFom = this.vedtakdatoFom,
        vedtakdatoTom = this.vedtakdatoTom,
        sats = this.sats,
        satsType = Utbetalingsperiode.SatsType.valueOf(this.satsType.name),
        utbetalesTil = this.utbetalesTil,
        behandlingId = this.behandlingId,
        utbetalingsgrad = this.utbetalingsgrad,
    )
