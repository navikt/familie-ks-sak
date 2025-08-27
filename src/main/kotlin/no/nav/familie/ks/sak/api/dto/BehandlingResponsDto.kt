package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.VenteÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class BehandlingResponsDto(
    val behandlingId: Long,
    val steg: BehandlingSteg,
    val stegTilstand: List<BehandlingStegTilstandResponsDto>,
    val status: BehandlingStatus,
    val resultat: Behandlingsresultat,
    val type: BehandlingType,
    val kategori: BehandlingKategori,
    val årsak: BehandlingÅrsak,
    val opprettetTidspunkt: LocalDateTime,
    val aktivertTidspunkt: LocalDateTime,
    val endretAv: String,
    val arbeidsfordelingPåBehandling: ArbeidsfordelingResponsDto,
    val søknadsgrunnlag: SøknadDto?,
    val behandlingPåVent: BehandlingPåVentDto?,
    val personer: List<PersonResponsDto>,
    val personResultater: List<PersonResultatResponsDto>,
    val utbetalingsperioder: List<UtbetalingsperiodeResponsDto>,
    val personerMedAndelerTilkjentYtelse: List<PersonerMedAndelerResponsDto>,
    val vedtak: VedtakDto?,
    val endretUtbetalingAndeler: List<EndretUtbetalingAndelResponsDto>,
    val overgangsordningAndeler: List<OvergangsordningAndelDto>,
    val totrinnskontroll: TotrinnskontrollDto?,
    val endringstidspunkt: LocalDate?,
    val sisteVedtaksperiodeVisningDato: LocalDate?,
    val tilbakekreving: TilbakekrevingResponsDto?,
    val kompetanser: List<KompetanseDto>,
    val utenlandskePeriodebeløp: List<UtenlandskPeriodebeløpDto>,
    val valutakurser: List<ValutakursDto>,
    val feilutbetaltValuta: List<FeilutbetaltValutaDto> = emptyList(),
    val refusjonEøs: List<RefusjonEøsDto> = emptyList(),
    val korrigertEtterbetaling: KorrigertEtterbetalingResponsDto?,
    val korrigertVedtak: KorrigertVedtakResponsDto?,
    val brevmottakere: List<BrevmottakerDto> = emptyList(),
    val søknadMottattDato: LocalDateTime?,
)

data class BehandlingStegTilstandResponsDto(
    val behandlingSteg: BehandlingSteg,
    val behandlingStegStatus: BehandlingStegStatus,
    val årsak: VenteÅrsak?,
    val frist: LocalDate?,
)

data class ArbeidsfordelingResponsDto(
    val behandlendeEnhetId: String,
    val behandlendeEnhetNavn: String,
    val manueltOverstyrt: Boolean = false,
)

data class PersonResponsDto(
    val type: PersonType,
    val fødselsdato: LocalDate?,
    val personIdent: String,
    val navn: String,
    val kjønn: KJOENN,
    val registerhistorikk: RegisterHistorikkResponsDto? = null,
    val målform: Målform,
    val dødsfallDato: LocalDate? = null,
    val adopsjonsdato: LocalDate? = null,
)

data class RegisterHistorikkResponsDto(
    val hentetTidspunkt: LocalDateTime,
    val sivilstand: List<RegisteropplysningResponsDto>? = emptyList(),
    val oppholdstillatelse: List<RegisteropplysningResponsDto>? = emptyList(),
    val statsborgerskap: List<RegisteropplysningResponsDto>? = emptyList(),
    val bostedsadresse: List<RegisteropplysningResponsDto>? = emptyList(),
    val oppholdsadresse: List<RegisteropplysningResponsDto>? = emptyList(),
    val dødsboadresse: List<RegisteropplysningResponsDto>? = emptyList(),
)

data class RegisteropplysningResponsDto(
    val fom: LocalDate?,
    val tom: LocalDate?,
    var verdi: String,
)

data class PersonerMedAndelerResponsDto(
    val personIdent: String?,
    val beløp: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val ytelsePerioder: List<YtelsePerioderDto>,
)

data class YtelsePerioderDto(
    val beløp: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val ytelseType: YtelseType,
    val skalUtbetales: Boolean,
)

data class TilbakekrevingResponsDto(
    val valg: Tilbakekrevingsvalg,
    val varsel: String? = null,
    val begrunnelse: String,
    val tilbakekrevingsbehandlingId: String? = null,
)
