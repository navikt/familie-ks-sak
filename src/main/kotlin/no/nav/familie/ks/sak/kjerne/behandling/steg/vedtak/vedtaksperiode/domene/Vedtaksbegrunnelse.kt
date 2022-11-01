package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.eksterne.kontrakter.AnnenForeldersAktivitet
import no.nav.familie.eksterne.kontrakter.SøkersAktivitet
import no.nav.familie.ks.sak.common.util.NullablePeriode
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.Standardbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.formaterBeløp
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.hentMånedOgÅrForBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.tilBrevTekst
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ks.sak.kjerne.brev.domene.MinimertRestEndretAndel
import no.nav.familie.ks.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.brev.domene.beløpUtbetaltFor
import no.nav.familie.ks.sak.kjerne.brev.domene.totaltUtbetalt
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "Vedtaksbegrunnelse")
@Table(name = "VEDTAKSBEGRUNNELSE")
class Vedtaksbegrunnelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vedtaksbegrunnelse_seq_generator")
    @SequenceGenerator(
        name = "vedtaksbegrunnelse_seq_generator",
        sequenceName = "vedtaksbegrunnelse_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_vedtaksperiode_id", nullable = false, updatable = false)
    val vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,

    @Enumerated(EnumType.STRING)
    @Column(name = "vedtak_begrunnelse_spesifikasjon", updatable = false)
    val standardbegrunnelse: Standardbegrunnelse
) {

    fun kopier(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): Vedtaksbegrunnelse = Vedtaksbegrunnelse(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        standardbegrunnelse = this.standardbegrunnelse
    )

    override fun toString(): String {
        return "Vedtaksbegrunnelse(id=$id, standardbegrunnelse=$standardbegrunnelse)"
    }
}

fun Vedtaksbegrunnelse.tilVedtaksbegrunnelseDto() = VedtaksbegrunnelseDto(
    standardbegrunnelse = this.standardbegrunnelse.enumnavnTilString(),
    vedtakBegrunnelseType = this.standardbegrunnelse.vedtakBegrunnelseType,
    vedtakBegrunnelseSpesifikasjon = this.standardbegrunnelse.enumnavnTilString()
)

enum class Begrunnelsetype {
    STANDARD_BEGRUNNELSE,
    EØS_BEGRUNNELSE,
    FRITEKST
}

interface Begrunnelse : Comparable<Begrunnelse> {
    val type: Begrunnelsetype
    val vedtakBegrunnelseType: VedtakBegrunnelseType?

    override fun compareTo(other: Begrunnelse): Int {
        return when {
            this.type == Begrunnelsetype.FRITEKST -> Int.MAX_VALUE
            other.type == Begrunnelsetype.FRITEKST -> -Int.MAX_VALUE
            this.vedtakBegrunnelseType == null -> Int.MAX_VALUE
            other.vedtakBegrunnelseType == null -> -Int.MAX_VALUE

            else -> this.vedtakBegrunnelseType!!.sorteringsrekkefølge - other.vedtakBegrunnelseType!!.sorteringsrekkefølge
        }
    }
}

interface BegrunnelseMedData : Begrunnelse {
    val apiNavn: String
}

data class BegrunnelseData(
    override val vedtakBegrunnelseType: VedtakBegrunnelseType,
    override val apiNavn: String,

    val gjelderSoker: Boolean,
    val barnasFodselsdatoer: String,
    val fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling: String,
    val fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling: String,
    val antallBarn: Int,
    val antallBarnOppfyllerTriggereOgHarUtbetaling: Int,
    val antallBarnOppfyllerTriggereOgHarNullutbetaling: Int,
    val maanedOgAarBegrunnelsenGjelderFor: String?,
    val maalform: String,
    val belop: String,
    val soknadstidspunkt: String,
    val avtaletidspunktDeltBosted: String,
) : BegrunnelseMedData {
    override val type: Begrunnelsetype = Begrunnelsetype.STANDARD_BEGRUNNELSE
}

data class FritekstBegrunnelse(
    val fritekst: String
) : Begrunnelse {
    override val vedtakBegrunnelseType: VedtakBegrunnelseType? = null
    override val type: Begrunnelsetype = Begrunnelsetype.FRITEKST
}

data class EØSBegrunnelseData(
    override val vedtakBegrunnelseType: VedtakBegrunnelseType,
    override val apiNavn: String,

    val annenForeldersAktivitet: AnnenForeldersAktivitet,
    val annenForeldersAktivitetsland: String?,
    val barnetsBostedsland: String,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maalform: String,
    val sokersAktivitet: SøkersAktivitet,
    val sokersAktivitetsland: String?
) : BegrunnelseMedData {
    override val type: Begrunnelsetype = Begrunnelsetype.EØS_BEGRUNNELSE
}

fun BrevBegrunnelseGrunnlagMedPersoner.tilBrevBegrunnelse(
    vedtaksperiode: NullablePeriode,
    personerIPersongrunnlag: List<MinimertRestPerson>,
    brevMålform: Målform,
    uregistrerteBarn: List<MinimertUregistrertBarn>,
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>,
    minimerteRestEndredeAndeler: List<MinimertRestEndretAndel>
): Begrunnelse {
    val personerPåBegrunnelse =
        personerIPersongrunnlag.filter { person -> this.personIdenter.contains(person.personIdent) }

    val barnSomOppfyllerTriggereOgHarUtbetaling = personerPåBegrunnelse.filter { person ->
        person.type == PersonType.BARN && minimerteUtbetalingsperiodeDetaljer.any { it.utbetaltPerMnd > 0 && it.person.personIdent == person.personIdent }
    }
    val barnSomOppfyllerTriggereOgHarNullutbetaling = personerPåBegrunnelse.filter { person ->
        person.type == PersonType.BARN && minimerteUtbetalingsperiodeDetaljer.any { it.utbetaltPerMnd == 0 && it.person.personIdent == person.personIdent }
    }

    val gjelderSøker = personerPåBegrunnelse.any { it.type == PersonType.SØKER }

    val barnasFødselsdatoer = this.hentBarnasFødselsdagerForBegrunnelse(
        uregistrerteBarn = uregistrerteBarn,
        personerIBehandling = personerIPersongrunnlag,
        personerPåBegrunnelse = personerPåBegrunnelse,
        personerMedUtbetaling = minimerteUtbetalingsperiodeDetaljer.map { it.person },
        gjelderSøker = gjelderSøker
    )

    val antallBarn = this.hentAntallBarnForBegrunnelse(
        uregistrerteBarn = uregistrerteBarn,
        gjelderSøker = gjelderSøker,
        barnasFødselsdatoer = barnasFødselsdatoer
    )

    val månedOgÅrBegrunnelsenGjelderFor =
        if (vedtaksperiode.fom == null) {
            null
        } else {
            this.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                periode = Periode(
                    fom = vedtaksperiode.fom,
                    tom = vedtaksperiode.tom ?: TIDENES_ENDE
                )
            )
        }

    val beløp = this.hentBeløp(gjelderSøker, minimerteUtbetalingsperiodeDetaljer)

    val endringsperioder = this.standardbegrunnelse.hentRelevanteEndringsperioderForBegrunnelse(
        minimerteRestEndredeAndeler = minimerteRestEndredeAndeler,
        vedtaksperiode = vedtaksperiode
    )

    val søknadstidspunkt = endringsperioder.sortedBy { it.søknadstidspunkt }
        .firstOrNull { this.triggesAv.endringsaarsaker.contains(it.årsak) }?.søknadstidspunkt

    this.validerBrevbegrunnelse(
        gjelderSøker = gjelderSøker,
        barnasFødselsdatoer = barnasFødselsdatoer
    )

    return BegrunnelseData(
        gjelderSoker = gjelderSøker,
        barnasFodselsdatoer = barnasFødselsdatoer.tilBrevTekst(),
        fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling = barnSomOppfyllerTriggereOgHarUtbetaling.map { it.fødselsdato }
            .tilBrevTekst(),
        fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling = barnSomOppfyllerTriggereOgHarNullutbetaling.map { it.fødselsdato }
            .tilBrevTekst(),
        antallBarn = antallBarn,
        antallBarnOppfyllerTriggereOgHarUtbetaling = barnSomOppfyllerTriggereOgHarUtbetaling.size,
        antallBarnOppfyllerTriggereOgHarNullutbetaling = barnSomOppfyllerTriggereOgHarNullutbetaling.size,
        maanedOgAarBegrunnelsenGjelderFor = månedOgÅrBegrunnelsenGjelderFor,
        maalform = brevMålform.tilSanityFormat(),
        apiNavn = this.standardbegrunnelse.sanityApiNavn,
        belop = formaterBeløp(beløp),
        soknadstidspunkt = søknadstidspunkt?.tilKortString() ?: "",
        avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted?.tilKortString() ?: "",
        vedtakBegrunnelseType = this.vedtakBegrunnelseType
    )
}

fun Standardbegrunnelse.hentRelevanteEndringsperioderForBegrunnelse(
    minimerteRestEndredeAndeler: List<MinimertRestEndretAndel>,
    vedtaksperiode: NullablePeriode
) = when (this.vedtakBegrunnelseType) {
    VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING -> {
        minimerteRestEndredeAndeler.filter {
            it.periode.tom.sisteDagIInneværendeMåned()
                ?.erDagenFør(vedtaksperiode.fom?.førsteDagIInneværendeMåned()) == true
        }
    }
    VedtakBegrunnelseType.ENDRET_UTBETALING -> {
        minimerteRestEndredeAndeler.filter { it.erOverlappendeMed(vedtaksperiode.tilNullableMånedPeriode()) }
    }
    else -> emptyList()
}

private fun BrevBegrunnelseGrunnlagMedPersoner.validerBrevbegrunnelse(
    gjelderSøker: Boolean,
    barnasFødselsdatoer: List<LocalDate>
) {
    if (!gjelderSøker && barnasFødselsdatoer.isEmpty() &&
        !this.triggesAv.satsendring &&
        this.standardbegrunnelse != Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN
    ) {
        throw IllegalStateException("Ingen personer på brevbegrunnelse")
    }
}

private fun BrevBegrunnelseGrunnlagMedPersoner.hentBeløp(
    gjelderSøker: Boolean,
    minimerteUtbetalingsperiodeDetaljer: List<MinimertUtbetalingsperiodeDetalj>
) = if (gjelderSøker) {
    if (this.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG ||
        this.vedtakBegrunnelseType == VedtakBegrunnelseType.OPPHØR
    ) {
        0
    } else {
        minimerteUtbetalingsperiodeDetaljer.totaltUtbetalt()
    }
} else {
    minimerteUtbetalingsperiodeDetaljer.beløpUtbetaltFor(this.personIdenter)
}
