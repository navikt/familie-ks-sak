package no.nav.familie.ks.sak.kjerne.behandling.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
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
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.VenteÅrsak
import java.time.LocalDate
import java.util.Objects

@Entity(name = "BehandlingStegTilstand")
@Table(name = "BEHANDLING_STEG_TILSTAND")
data class BehandlingStegTilstand(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_steg_tilstand_seq_generator")
    @SequenceGenerator(
        name = "behandling_steg_tilstand_seq_generator",
        sequenceName = "behandling_steg_tilstand_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    @JsonIgnore
    val behandling: Behandling,
    @Enumerated(EnumType.STRING)
    @Column(name = "behandling_steg", nullable = false)
    val behandlingSteg: BehandlingSteg,
    @Enumerated(EnumType.STRING)
    @Column(name = "behandling_steg_status", nullable = false)
    var behandlingStegStatus: BehandlingStegStatus = BehandlingStegStatus.KLAR,
    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak")
    var årsak: VenteÅrsak? = null,
    @Column(name = "frist")
    var frist: LocalDate? = null,
) : BaseEntitet() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BehandlingStegTilstand

        return behandlingSteg == other.behandlingSteg
    }

    override fun hashCode(): Int = Objects.hash(behandlingSteg)

    override fun toString(): String = "BehandlingStegTilstand(id=$id, behandling=${behandling.id}, behandlingSteg=$behandlingSteg, behandlingStegStatus=$behandlingStegStatus)"
}
