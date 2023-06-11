package no.nav.familie.ks.sak.kjerne.logg.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.logg.LoggType
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import java.time.LocalDateTime

@Entity(name = "Logg")
@Table(name = "logg")
data class Logg(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "logg_seq_generator")
    @SequenceGenerator(name = "logg_seq_generator", sequenceName = "logg_seq", allocationSize = 50)
    val id: Long = 0,

    @Column(name = "opprettet_av", nullable = false, updatable = false)
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerNavn(),

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "fk_behandling_id")
    val behandlingId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    val type: LoggType,

    @Column(name = "tittel")
    val tittel: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle")
    val rolle: BehandlerRolle,

    /**
     * Feltet støtter markdown frontend.
     */
    @Column(name = "tekst")
    val tekst: String = ""
) {

    constructor(behandlingId: Long, type: LoggType, rolle: BehandlerRolle, tekst: String = "") : this(
        behandlingId = behandlingId,
        type = type,
        tittel = type.tittel,
        rolle = rolle,
        tekst = tekst
    )
}
