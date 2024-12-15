package no.nav.familie.ks.sak.kjerne.endretutbetaling.domene

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelRequestDto
import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelResponsDto
import no.nav.familie.ks.sak.api.dto.mapTilBegrunnelser
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.YearMonthConverter
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelseListConverter
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Entity(name = "EndretUtbetalingAndel")
@Table(name = "ENDRET_UTBETALING_ANDEL")
data class EndretUtbetalingAndel(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "endret_utbetaling_andel_seq_generator")
    @SequenceGenerator(
        name = "endret_utbetaling_andel_seq_generator",
        sequenceName = "endret_utbetaling_andel_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @ManyToOne
    @JoinColumn(name = "fk_po_person_id")
    var person: Person? = null,
    @Column(name = "prosent")
    var prosent: BigDecimal? = null,
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var fom: YearMonth? = null,
    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var tom: YearMonth? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak")
    var årsak: Årsak? = null,
    @Column(name = "avtaletidspunkt_delt_bosted")
    var avtaletidspunktDeltBosted: LocalDate? = null,
    @Column(name = "soknadstidspunkt")
    var søknadstidspunkt: LocalDate? = null,
    @Column(name = "begrunnelse")
    var begrunnelse: String? = null,
    @Column(name = "vedtak_begrunnelse_spesifikasjoner")
    @Convert(converter = IBegrunnelseListConverter::class)
    var begrunnelser: List<NasjonalEllerFellesBegrunnelse> = emptyList(),
    @Column(name = "er_eksplisitt_avslag_paa_soknad")
    var erEksplisittAvslagPåSøknad: Boolean? = null,
) : BaseEntitet() {
    fun overlapperMed(periode: MånedPeriode) = periode.overlapperHeltEllerDelvisMed(this.periode)

    val periode
        get(): MånedPeriode {
            validerUtfyltEndring()
            return MånedPeriode(checkNotNull(this.fom), checkNotNull(this.tom))
        }

    fun validerUtfyltEndring() {
        if (this.manglerObligatoriskFelt() || (begrunnelse?.isEmpty() == true)
        ) {
            val feilmelding =
                "Person, prosent, fom, tom, årsak, begrunnese og søknadstidspunkt skal være utfylt: $this"
            throw FunksjonellFeil(melding = feilmelding, frontendFeilmelding = feilmelding)
        }

        if (checkNotNull(fom) > checkNotNull(tom)) {
            throw FunksjonellFeil(
                melding = "fom må være lik eller komme før tom",
                frontendFeilmelding = "Du kan ikke sette en f.o.m. dato som er etter t.o.m. dato",
            )
        }

        if (årsak == Årsak.DELT_BOSTED && avtaletidspunktDeltBosted == null) {
            throw FunksjonellFeil("Avtaletidspunkt skal være utfylt når årsak er delt bosted: $this")
        }
    }

    fun manglerObligatoriskFelt() =
        listOf(
            this.person,
            this.prosent,
            this.fom,
            this.tom,
            this.årsak,
            this.søknadstidspunkt,
        ).any { it == null }

    fun erÅrsakDeltBosted() = this.årsak == Årsak.DELT_BOSTED
}

fun EndretUtbetalingAndelMedAndelerTilkjentYtelse.tilEndretUtbetalingAndelResponsDto() =
    EndretUtbetalingAndelResponsDto(
        id = this.id,
        personIdent = this.aktivtFødselsnummer,
        prosent = this.prosent,
        fom = this.fom,
        tom = this.tom,
        årsak = this.årsak,
        avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted,
        søknadstidspunkt = this.søknadstidspunkt,
        begrunnelse = this.begrunnelse,
        begrunnelser = this.begrunnelser,
        erEksplisittAvslagPåSøknad = this.erEksplisittAvslagPåSøknad,
        erTilknyttetAndeler = this.andelerTilkjentYtelse.isNotEmpty(),
    )

fun EndretUtbetalingAndel.fraEndretUtbetalingAndelRequestDto(
    endretUtbetalingAndelRequestDto: EndretUtbetalingAndelRequestDto,
    person: Person,
): EndretUtbetalingAndel {
    this.person = person
    this.prosent = endretUtbetalingAndelRequestDto.prosent
    this.fom = endretUtbetalingAndelRequestDto.fom
    this.tom = endretUtbetalingAndelRequestDto.tom
    this.årsak = endretUtbetalingAndelRequestDto.årsak
    this.avtaletidspunktDeltBosted = endretUtbetalingAndelRequestDto.avtaletidspunktDeltBosted
    this.søknadstidspunkt = endretUtbetalingAndelRequestDto.søknadstidspunkt
    this.begrunnelse = endretUtbetalingAndelRequestDto.begrunnelse
    this.begrunnelser = endretUtbetalingAndelRequestDto.mapTilBegrunnelser()
    this.erEksplisittAvslagPåSøknad = endretUtbetalingAndelRequestDto.erEksplisittAvslagPåSøknad
    return this
}

enum class Årsak(
    val visningsnavn: String,
) {
    DELT_BOSTED("Delt bosted"),
    ETTERBETALING_3MND("Etterbetaling 3 måneder"),
    ENDRE_MOTTAKER("Foreldrene bor sammen, endret mottaker"),
    ALLEREDE_UTBETALT("Allerede utbetalt"),
    FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024("Fulltidsplass i barnehage august 2024"),
}

sealed interface IEndretUtbetalingAndel

data class TomEndretUtbetalingAndel(
    val id: Long,
    val behandlingId: Long,
) : IEndretUtbetalingAndel

sealed interface IUtfyltEndretUtbetalingAndel : IEndretUtbetalingAndel {
    val id: Long
    val behandlingId: Long
    val person: Person
    val prosent: BigDecimal
    val fom: YearMonth
    val tom: YearMonth
    val årsak: Årsak
    val søknadstidspunkt: LocalDate
    val begrunnelse: String
}

data class UtfyltEndretUtbetalingAndel(
    override val id: Long,
    override val behandlingId: Long,
    override val person: Person,
    override val prosent: BigDecimal,
    override val fom: YearMonth,
    override val tom: YearMonth,
    override val årsak: Årsak,
    override val søknadstidspunkt: LocalDate,
    override val begrunnelse: String,
) : IUtfyltEndretUtbetalingAndel

data class UtfyltEndretUtbetalingAndelDeltBosted(
    override val id: Long,
    override val behandlingId: Long,
    override val person: Person,
    override val prosent: BigDecimal,
    override val fom: YearMonth,
    override val tom: YearMonth,
    override val årsak: Årsak,
    override val søknadstidspunkt: LocalDate,
    override val begrunnelse: String,
    val avtaletidspunktDeltBosted: LocalDate,
) : IUtfyltEndretUtbetalingAndel

fun EndretUtbetalingAndel.tilIEndretUtbetalingAndel(): IEndretUtbetalingAndel =
    if (this.manglerObligatoriskFelt()) {
        TomEndretUtbetalingAndel(
            this.id,
            this.behandlingId,
        )
    } else {
        if (this.erÅrsakDeltBosted()) {
            UtfyltEndretUtbetalingAndelDeltBosted(
                id = this.id,
                behandlingId = this.behandlingId,
                person = this.person!!,
                prosent = this.prosent!!,
                fom = this.fom!!,
                tom = this.tom!!,
                årsak = this.årsak!!,
                avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted!!,
                søknadstidspunkt = this.søknadstidspunkt!!,
                begrunnelse = this.begrunnelse!!,
            )
        } else {
            UtfyltEndretUtbetalingAndel(
                id = this.id,
                behandlingId = this.behandlingId,
                person = this.person!!,
                prosent = this.prosent!!,
                fom = this.fom!!,
                tom = this.tom!!,
                årsak = this.årsak!!,
                søknadstidspunkt = this.søknadstidspunkt!!,
                begrunnelse = this.begrunnelse!!,
            )
        }
    }
