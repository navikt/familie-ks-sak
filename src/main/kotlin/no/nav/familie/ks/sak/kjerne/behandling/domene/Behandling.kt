package no.nav.familie.ks.sak.kjerne.behandling.domene

import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType.REVURDERING
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType.TEKNISK_ENDRING
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.IKKE_VURDERT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype as OppgaveBehandlingType

@Entity(name = "Behandling")
@Table(name = "BEHANDLING")
data class Behandling(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_seq_generator")
    @SequenceGenerator(name = "behandling_seq_generator", sequenceName = "behandling_seq", allocationSize = 50)
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_fagsak_id", nullable = false, updatable = false)
    val fagsak: Fagsak,

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat", nullable = false)
    var resultat: Behandlingsresultat = IKKE_VURDERT,

    @Enumerated(EnumType.STRING)
    @Column(name = "behandling_type", nullable = false)
    val type: BehandlingType,

    @Enumerated(EnumType.STRING)
    @Column(name = "opprettet_aarsak", nullable = false)
    val opprettetÅrsak: BehandlingÅrsak,

    @Enumerated(EnumType.STRING)
    @Column(name = "kategori", nullable = false, updatable = true)
    var kategori: BehandlingKategori,

    @OneToMany(mappedBy = "behandling", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    val behandlingStegTilstand: MutableSet<BehandlingStegTilstand> = mutableSetOf(),

    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: BehandlingStatus = initStatus(),

    @Column(name = "soknad_mottatt_dato")
    var søknadMottattDato: LocalDateTime? = null,

    var overstyrtEndringstidspunkt: LocalDate? = null

) : BaseEntitet() {

    override fun toString(): String {
        return "Behandling(" +
            "id=$id, " +
            "fagsak=${fagsak.id}, " +
            "type=$type, " +
            "kategori=$kategori, " +
            "status=$status, " +
            "resultat=$resultat)"
    }

    fun validerBehandlingstype(sisteBehandlingSomErVedtatt: Behandling? = null) {
        if (type !in opprettetÅrsak.gyldigeBehandlingstyper) {
            throw Feil("Behandling med $type og årsak $opprettetÅrsak samsvarer ikke.")
        }

        if (type == REVURDERING && sisteBehandlingSomErVedtatt == null) {
            throw Feil("Kan ikke opprette revurdering på $fagsak uten noen andre behandlinger som er vedtatt.")
        }
    }

    val steg: BehandlingSteg
        get() = behandlingStegTilstand.singleOrNull {
            it.behandlingStegStatus == BehandlingStegStatus.KLAR
        }?.behandlingSteg ?: behandlingStegTilstand.maxBy { it.opprettetTidspunkt }.behandlingSteg

    fun initBehandlingStegTilstand(): Behandling {
        behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = this,
                behandlingSteg = BehandlingSteg.REGISTRERE_PERSONGRUNNLAG
            )
        )
        return this
    }

    fun leggTilNesteSteg(behandlingSteg: BehandlingSteg): Behandling {
        behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = this,
                behandlingSteg = behandlingSteg
            )
        )
        return this
    }

    fun opprettBehandleSakOppgave(): Boolean {
        return type != TEKNISK_ENDRING
    }

    fun skalSendeVedtaksbrev(): Boolean {
        return when {
            type == TEKNISK_ENDRING -> false
            opprettetÅrsak == BehandlingÅrsak.SATSENDRING -> false
            else -> true
        }
    }

    fun erHenlagt() =
        resultat in listOf(HENLAGT_FEILAKTIG_OPPRETTET, HENLAGT_SØKNAD_TRUKKET, HENLAGT_TEKNISK_VEDLIKEHOLD)

    fun erVedtatt() = status == BehandlingStatus.AVSLUTTET && !erHenlagt()

    fun erAvsluttet() = status == BehandlingStatus.AVSLUTTET

    fun erSøknad() = opprettetÅrsak == BehandlingÅrsak.SØKNAD

    fun erKlage() = this.opprettetÅrsak == BehandlingÅrsak.KLAGE

    fun erSatsendring() = this.opprettetÅrsak == BehandlingÅrsak.SATSENDRING

    fun erTekniskEndring() = opprettetÅrsak == BehandlingÅrsak.TEKNISK_ENDRING

    fun erKorrigereVedtak() = opprettetÅrsak == BehandlingÅrsak.KORREKSJON_VEDTAKSBREV
}

/**
 * Enum for de ulike hovedresultatene en behandling kan ha.
 *
 * Et behandlingsresultater beskriver det samlede resultatet for vurderinger gjort i inneværende behandling.
 * @displayName benyttes for visning av resultat
 */
enum class Behandlingsresultat(val displayName: String, val gyldigeBehandlingstyper: List<BehandlingType>) {

    // Søknad
    INNVILGET(displayName = "Innvilget", BehandlingType.values().toList()),
    INNVILGET_OG_OPPHØRT(displayName = "Innvilget og opphørt", BehandlingType.values().toList()),
    INNVILGET_OG_ENDRET(displayName = "Innvilget og endret", listOf(REVURDERING, TEKNISK_ENDRING)),
    INNVILGET_ENDRET_OG_OPPHØRT(displayName = "Innvilget, endret og opphørt", listOf(REVURDERING, TEKNISK_ENDRING)),

    DELVIS_INNVILGET(displayName = "Delvis innvilget", BehandlingType.values().toList()),
    DELVIS_INNVILGET_OG_OPPHØRT(displayName = "Delvis innvilget og opphørt", BehandlingType.values().toList()),
    DELVIS_INNVILGET_OG_ENDRET(displayName = "Delvis innvilget og endret", listOf(REVURDERING, TEKNISK_ENDRING)),
    DELVIS_INNVILGET_ENDRET_OG_OPPHØRT(
        displayName = "Delvis innvilget, endret og opphørt",
        listOf(REVURDERING, TEKNISK_ENDRING)
    ),

    AVSLÅTT(displayName = "Avslått", BehandlingType.values().toList()),
    AVSLÅTT_OG_OPPHØRT(displayName = "Avslått og opphørt", listOf(REVURDERING, TEKNISK_ENDRING)),
    AVSLÅTT_OG_ENDRET(displayName = "Avslått og endret", listOf(REVURDERING, TEKNISK_ENDRING)),
    AVSLÅTT_ENDRET_OG_OPPHØRT(displayName = "Avslått, endret og opphørt", listOf(REVURDERING, TEKNISK_ENDRING)),

    // Revurdering uten søknad
    ENDRET_UTBETALING(displayName = "Endret utbetaling", listOf(REVURDERING, TEKNISK_ENDRING)),
    ENDRET_UTEN_UTBETALING(displayName = "Endret, uten endret utbetaling", listOf(REVURDERING, TEKNISK_ENDRING)),
    ENDRET_OG_OPPHØRT(displayName = "Endret og opphørt", listOf(REVURDERING, TEKNISK_ENDRING)),
    OPPHØRT(displayName = "Opphørt", BehandlingType.values().toList()),
    FORTSATT_OPPHØRT(displayName = "Fortsatt opphørt", listOf(REVURDERING, TEKNISK_ENDRING)),
    FORTSATT_INNVILGET(displayName = "Fortsatt innvilget", listOf(REVURDERING, TEKNISK_ENDRING)),

    // Henlagt
    HENLAGT_FEILAKTIG_OPPRETTET(displayName = "Henlagt feilaktig opprettet", BehandlingType.values().toList()),
    HENLAGT_SØKNAD_TRUKKET(displayName = "Henlagt søknad trukket", BehandlingType.values().toList()),
    HENLAGT_TEKNISK_VEDLIKEHOLD(displayName = "Henlagt teknisk vedlikehold", BehandlingType.values().toList()),

    IKKE_VURDERT(displayName = "Ikke vurdert", emptyList());

    fun kanIkkeSendesTilOppdrag(): Boolean =
        this in listOf(FORTSATT_INNVILGET, AVSLÅTT, FORTSATT_OPPHØRT, ENDRET_UTEN_UTBETALING)

    fun erAvslått(): Boolean = this in listOf(AVSLÅTT, AVSLÅTT_OG_OPPHØRT, AVSLÅTT_OG_ENDRET, AVSLÅTT_ENDRET_OG_OPPHØRT)
}

fun Behandlingsresultat.tilDokumenttype() = when (this) {
    Behandlingsresultat.AVSLÅTT -> Dokumenttype.KONTANTSTØTTE_VEDTAK_AVSLAG
    Behandlingsresultat.OPPHØRT -> Dokumenttype.KONTANTSTØTTE_OPPHØR
    else -> Dokumenttype.KONTANTSTØTTE_VEDTAK_INNVILGELSE
}

/**
 * Årsak er knyttet til en behandling og sier noe om hvorfor behandling ble opprettet.
 */
enum class BehandlingÅrsak(val visningsnavn: String, val gyldigeBehandlingstyper: List<BehandlingType>) {

    SØKNAD("Søknad", listOf(FØRSTEGANGSBEHANDLING, REVURDERING)),
    ÅRLIG_KONTROLL("Årsak kontroll", listOf(REVURDERING)),
    DØDSFALL("Dødsfall", listOf(REVURDERING)),
    NYE_OPPLYSNINGER("Nye opplysninger", listOf(REVURDERING)),
    KLAGE("Klage", listOf(REVURDERING)),
    TEKNISK_ENDRING(
        "Teknisk endring",
        listOf(BehandlingType.TEKNISK_ENDRING)
    ), // Brukes i tilfeller ved systemfeil og vi ønsker å iverksette mot OS på nytt
    KORREKSJON_VEDTAKSBREV("Korrigere vedtak med egen brevmal", listOf(REVURDERING)),
    SATSENDRING("Satsendring", listOf(REVURDERING)),
    BARNEHAGELISTE("Barnehageliste", listOf(REVURDERING));

    fun årsakSomKanEndreBehandlingKategori(): Boolean =
        this == SØKNAD || this == ÅRLIG_KONTROLL || this == NYE_OPPLYSNINGER ||
            this == KLAGE
}

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    REVURDERING("Revurdering"),
    TEKNISK_ENDRING("Teknisk endring")
}

enum class BehandlingKategori(val visningsnavn: String, val nivå: Int) {
    EØS("EØS", 2),
    NASJONAL("Nasjonal", 1);

    fun tilOppgavebehandlingType(): OppgaveBehandlingType {
        return when (this) {
            EØS -> OppgaveBehandlingType.EØS
            NASJONAL -> OppgaveBehandlingType.NASJONAL
        }
    }
}

fun List<BehandlingKategori>.finnHøyesteKategori(): BehandlingKategori? = this.maxByOrNull { it.nivå }

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    AVSLUTTET;

    fun erLåstMenIkkeAvsluttet() = this == FATTER_VEDTAK || this == IVERKSETTER_VEDTAK

    fun erÅpen(): Boolean = this != AVSLUTTET
}

fun initStatus(): BehandlingStatus = BehandlingStatus.UTREDES

enum class Beslutning {
    GODKJENT,
    UNDERKJENT;

    fun erGodkjent() = this == GODKJENT
}
